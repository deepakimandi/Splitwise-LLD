package com.lld.splitwise.settlement;

import com.lld.splitwise.model.Transaction;
import com.lld.splitwise.model.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * "Simplify debts" / minimise cash flow.
 *
 * 1. Only NET balances matter: who owes whom is irrelevant, only how much each person is up or down.
 *    (A owes B 10, B owes C 10  ==  A owes C 10.)
 * 2. Greedy with two heaps: repeatedly match the biggest creditor with the biggest debtor and
 *    transfer min(credit, debt). Each step zeroes at least one person, so we need at most N-1 transfers.
 *
 * Complexity: O(N log N) for N users with a non-zero balance.
 * Finding the true minimum is NP-hard (it needs splitting people into zero-sum subgroups),
 * so greedy is the standard, good-enough answer.
 */
public class DebtSimplifier {

    /** Mutable holder so we can update the remaining amount between heap operations. */
    private static final class Party {
        final User user;
        BigDecimal remaining;   // always positive

        Party(User user, BigDecimal remaining) {
            this.user = user;
            this.remaining = remaining;
        }
    }

    public List<Transaction> simplify(Map<User, BigDecimal> netBalances) {
        Comparator<Party> biggestFirst = Comparator.comparing((Party p) -> p.remaining).reversed()
                .thenComparing(p -> p.user.getUserId());          // deterministic tie-break
        PriorityQueue<Party> creditors = new PriorityQueue<>(biggestFirst);
        PriorityQueue<Party> debtors = new PriorityQueue<>(biggestFirst);

        netBalances.forEach((user, net) -> {
            if (net.signum() > 0) creditors.add(new Party(user, net));
            else if (net.signum() < 0) debtors.add(new Party(user, net.negate()));
        });

        List<Transaction> result = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Party creditor = creditors.poll();
            Party debtor = debtors.poll();
            BigDecimal amount = creditor.remaining.min(debtor.remaining);

            result.add(new Transaction(debtor.user, creditor.user, amount));

            creditor.remaining = creditor.remaining.subtract(amount);
            debtor.remaining = debtor.remaining.subtract(amount);
            if (creditor.remaining.signum() > 0) creditors.add(creditor);   // re-insert with new priority
            if (debtor.remaining.signum() > 0) debtors.add(debtor);
        }
        return result;
    }
}
