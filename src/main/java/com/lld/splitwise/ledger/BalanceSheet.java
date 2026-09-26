package com.lld.splitwise.ledger;

import com.lld.splitwise.concurrency.LockManager;
import com.lld.splitwise.model.Expense;
import com.lld.splitwise.model.Payment;
import com.lld.splitwise.model.Split;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ledger of ONE group.
 *
 * balances[a][b] > 0  means  b owes a that amount.
 * The matrix is always kept antisymmetric: balances[b][a] == -balances[a][b].
 * Zero entries are removed to keep rows small.
 *
 * Thread safety (two layers):
 *  - Writes lock every involved user through LockManager (ordered => no deadlock).
 *    Only users touched by an expense are blocked; others proceed in parallel.
 *  - The maps are ConcurrentHashMaps because two writers holding DISJOINT locks can still
 *    add rows to the outer map at the same time, and single-value reads are lock-free.
 *
 * Each group owns its own BalanceSheet and LockManager, which is a natural partition:
 * expenses in different groups never contend.
 */
public class BalanceSheet {

    private final Map<String, Map<String, BigDecimal>> balances = new ConcurrentHashMap<>();
    private final LockManager lockManager = new LockManager();

    /**
     * Posts an expense. {@code precondition} runs while the locks are held, so checks such as
     * "all participants are still members" cannot go stale before the write (no check-then-act race).
     */
    public void applyExpense(Expense expense, Runnable precondition) {
        lockManager.withLocks(expense.involvedUserIds(), () -> {
            precondition.run();
            post(expense, false);
        });
    }

    /** Undo an expense (delete/edit) by posting the exact opposite entries. */
    public void revertExpense(Expense expense) {
        lockManager.withLocks(expense.involvedUserIds(), () -> post(expense, true));
    }

    /** from paid to => from's debt to "to" goes down, i.e. from is credited against to. */
    public void applyPayment(Payment payment, Runnable precondition) {
        String from = payment.from().getUserId();
        String to = payment.to().getUserId();
        lockManager.withLocks(List.of(from, to), () -> {
            precondition.run();
            credit(from, to, payment.amount());
        });
    }

    /**
     * Runs {@code action} only if the user's NET balance is zero, atomically w.r.t. writes on that user.
     * Net (not every pair) is the right test: after simplified settlements a user can be +x with one
     * member and -x with another, which is a circular leftover that nets to nothing.
     */
    public boolean runIfSettled(String userId, Runnable action) {
        return lockManager.withLocks(List.of(userId), () -> {
            BigDecimal net = rowOf(userId).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (net.signum() != 0) {
                return false;
            }
            action.run();
            return true;
        });
    }

    /** Lock-free read of one pair. Positive => {@code other} owes {@code userId}. */
    public BigDecimal getBalance(String userId, String other) {
        return rowOf(userId).getOrDefault(other, BigDecimal.ZERO);
    }

    /** Net position of one user: positive => gets back money, negative => owes. */
    public BigDecimal getNetBalance(String userId) {
        return lockManager.withLocks(List.of(userId),
                () -> rowOf(userId).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Consistent point-in-time copy of the given users' rows. Locks all of them (in order),
     * so no half-applied expense can be observed. Used by settlement.
     */
    public Map<String, Map<String, BigDecimal>> snapshot(Collection<String> userIds) {
        return lockManager.withLocks(userIds, () -> {
            Map<String, Map<String, BigDecimal>> copy = new HashMap<>();
            for (String id : userIds) {
                copy.put(id, Map.copyOf(rowOf(id)));
            }
            return copy;
        });
    }

    /** Net balance for each of the given users, from a consistent snapshot. */
    public Map<String, BigDecimal> netBalances(Set<String> userIds) {
        Map<String, BigDecimal> net = new HashMap<>();
        snapshot(userIds).forEach((id, row) ->
                net.put(id, row.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
        return net;
    }

    // ----- internals: callers must hold the locks of every user they touch -----

    private void post(Expense expense, boolean reverse) {
        String payer = expense.getPaidBy().getUserId();
        for (Split split : expense.getSplits()) {
            String participant = split.user().getUserId();
            if (participant.equals(payer)) {
                continue;                               // your own share creates no debt
            }
            BigDecimal amount = reverse ? split.amount().negate() : split.amount();
            credit(payer, participant, amount);         // participant owes payer
        }
    }

    /** debtor owes creditor {@code amount} more. Updates both directions. */
    private void credit(String creditor, String debtor, BigDecimal amount) {
        adjust(creditor, debtor, amount);
        adjust(debtor, creditor, amount.negate());
    }

    private void adjust(String a, String b, BigDecimal delta) {
        balances.computeIfAbsent(a, k -> new ConcurrentHashMap<>())
                .merge(b, delta, (oldV, d) -> {
                    BigDecimal sum = oldV.add(d);
                    return sum.signum() == 0 ? null : sum;   // null => entry removed
                });
    }

    private Map<String, BigDecimal> rowOf(String userId) {
        return balances.getOrDefault(userId, Map.of());
    }
}
