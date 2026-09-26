package com.lld.splitwise.model;

import com.lld.splitwise.exception.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * An immutable expense. Built with a Builder because it has many fields, several optional.
 * Invariant checked at build time: splits add up exactly to the total amount.
 */
public final class Expense {
    private final String expenseId;
    private final String groupId;
    private final String description;
    private final User paidBy;
    private final BigDecimal amount;
    private final SplitType splitType;
    private final List<Split> splits;
    private final User createdBy;
    private final Instant createdAt;

    private Expense(Builder b) {
        this.expenseId = b.expenseId;
        this.groupId = b.groupId;
        this.description = b.description;
        this.paidBy = b.paidBy;
        this.amount = b.amount;
        this.splitType = b.splitType;
        this.splits = List.copyOf(b.splits);          // defensive, unmodifiable copy
        this.createdBy = b.createdBy != null ? b.createdBy : b.paidBy;
        this.createdAt = b.createdAt != null ? b.createdAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public String getExpenseId()    { return expenseId; }
    public String getGroupId()      { return groupId; }
    public String getDescription()  { return description; }
    public User getPaidBy()         { return paidBy; }
    public BigDecimal getAmount()   { return amount; }
    public SplitType getSplitType() { return splitType; }
    public List<Split> getSplits()  { return splits; }
    public User getCreatedBy()      { return createdBy; }
    public Instant getCreatedAt()   { return createdAt; }

    /** Every user whose balance this expense touches (payer + participants). */
    public Set<String> involvedUserIds() {
        Set<String> ids = new TreeSet<>();
        ids.add(paidBy.getUserId());
        splits.forEach(s -> ids.add(s.user().getUserId()));
        return ids;
    }

    @Override
    public String toString() {
        return "Expense[" + expenseId + ", '" + description + "', " + amount + " paid by " + paidBy + "]";
    }

    public static final class Builder {
        private String expenseId;
        private String groupId;
        private String description = "";
        private User paidBy;
        private BigDecimal amount;
        private SplitType splitType;
        private List<Split> splits;
        private User createdBy;
        private Instant createdAt;

        public Builder expenseId(String v)     { this.expenseId = v; return this; }
        public Builder groupId(String v)       { this.groupId = v; return this; }
        public Builder description(String v)   { this.description = v; return this; }
        public Builder paidBy(User v)          { this.paidBy = v; return this; }
        public Builder amount(BigDecimal v)    { this.amount = v; return this; }
        public Builder splitType(SplitType v)  { this.splitType = v; return this; }
        public Builder splits(List<Split> v)   { this.splits = v; return this; }
        public Builder createdBy(User v)       { this.createdBy = v; return this; }
        public Builder createdAt(Instant v)    { this.createdAt = v; return this; }

        public Expense build() {
            Objects.requireNonNull(expenseId, "expenseId");
            Objects.requireNonNull(groupId, "groupId");
            Objects.requireNonNull(paidBy, "paidBy");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(splitType, "splitType");
            if (splits == null || splits.isEmpty()) {
                throw new ValidationException("Expense must have at least one split");
            }
            BigDecimal sum = splits.stream().map(Split::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(amount) != 0) {
                throw new ValidationException("Splits (" + sum + ") do not add up to amount (" + amount + ")");
            }
            return new Expense(this);
        }
    }
}
