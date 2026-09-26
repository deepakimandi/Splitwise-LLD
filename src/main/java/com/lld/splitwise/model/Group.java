package com.lld.splitwise.model;

import com.lld.splitwise.exception.ConflictException;
import com.lld.splitwise.exception.ValidationException;
import com.lld.splitwise.ledger.BalanceSheet;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Aggregate root (DDD): a group owns its members and its ledger.
 * Nobody touches the BalanceSheet directly; every change goes through the Group, which
 * enforces the invariants (only members can take part, a member can leave only when settled).
 *
 * Expenses themselves are stored in ExpenseRepository (not in a List here), so a group with
 * thousands of expenses doesn't have to be loaded into memory; they are paged by groupId.
 */
public class Group {
    private final String groupId;
    private final String name;
    private final User createdBy;
    private final Map<String, User> members = new ConcurrentHashMap<>();
    private final BalanceSheet ledger = new BalanceSheet();

    public Group(String groupId, String name, User createdBy) {
        this.groupId = groupId;
        this.name = name;
        this.createdBy = createdBy;
        members.put(createdBy.getUserId(), createdBy);
    }

    public String getGroupId() { return groupId; }
    public String getName()    { return name; }
    public User getCreatedBy() { return createdBy; }

    public Set<User> getMembers() { return Set.copyOf(members.values()); }

    public Set<String> getMemberIds() { return Set.copyOf(members.keySet()); }

    public boolean hasMember(String userId) { return members.containsKey(userId); }

    public void addMember(User user) {
        members.putIfAbsent(user.getUserId(), user);
    }

    /** A member can leave only when their balance in this group is zero. */
    public void removeMember(User user) {
        boolean removed = ledger.runIfSettled(user.getUserId(), () -> members.remove(user.getUserId()));
        if (!removed) {
            throw new ConflictException(user + " has an unsettled balance in group " + name);
        }
    }

    public void recordExpense(Expense expense) {
        if (!groupId.equals(expense.getGroupId())) {
            throw new ValidationException("Expense belongs to another group");
        }
        ledger.applyExpense(expense, () -> requireMembers(expense.involvedUserIds()));
    }

    public void revertExpense(Expense expense) {
        ledger.revertExpense(expense);
    }

    public void recordPayment(Payment payment) {
        ledger.applyPayment(payment,
                () -> requireMembers(Set.of(payment.from().getUserId(), payment.to().getUserId())));
    }

    // ----- read side -----

    /** Positive => {@code other} owes {@code userId}. */
    public BigDecimal balanceBetween(String userId, String other) {
        return ledger.getBalance(userId, other);
    }

    public BigDecimal netBalanceOf(String userId) {
        return ledger.getNetBalance(userId);
    }

    public Map<User, BigDecimal> netBalances() {
        Map<String, BigDecimal> net = ledger.netBalances(getMemberIds());
        return net.entrySet().stream()
                .filter(e -> members.containsKey(e.getKey()))
                .collect(Collectors.toMap(e -> members.get(e.getKey()), Map.Entry::getValue));
    }

    private void requireMembers(Collection<String> userIds) {
        for (String id : userIds) {
            if (!members.containsKey(id)) {
                throw new ValidationException("User " + id + " is not a member of group " + name);
            }
        }
    }
}
