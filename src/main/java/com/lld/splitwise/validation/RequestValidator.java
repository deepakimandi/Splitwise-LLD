package com.lld.splitwise.validation;

import com.lld.splitwise.dto.CreateExpenseRequest;
import com.lld.splitwise.dto.SettleUpRequest;
import com.lld.splitwise.exception.ValidationException;
import com.lld.splitwise.model.Money;
import com.lld.splitwise.model.SplitType;

import java.math.BigDecimal;

/**
 * Fail-fast SHAPE validation of incoming DTOs (nulls, positive amounts, known enum values).
 * Business rules (membership, sums, percentages) live in the domain and the strategies,
 * because they must hold no matter which API called them.
 */
public final class RequestValidator {

    private RequestValidator() {}

    public static SplitType validate(CreateExpenseRequest r) {
        require(r != null, "Request is required");
        requireText(r.idempotencyKey(), "idempotencyKey");
        requireText(r.requesterId(), "requesterId");
        requireText(r.groupId(), "groupId");
        requireText(r.paidByUserId(), "paidByUserId");
        requireAmount(r.amount());
        require(r.splits() != null && !r.splits().isEmpty(), "At least one split is required");
        try {
            return SplitType.valueOf(r.splitType());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Unknown split type: " + r.splitType());
        }
    }

    public static void validate(SettleUpRequest r) {
        require(r != null, "Request is required");
        requireText(r.idempotencyKey(), "idempotencyKey");
        requireText(r.groupId(), "groupId");
        requireText(r.fromUserId(), "fromUserId");
        requireText(r.toUserId(), "toUserId");
        require(!r.fromUserId().equals(r.toUserId()), "Cannot pay yourself");
        requireAmount(r.amount());
    }

    private static void requireAmount(BigDecimal amount) {
        require(Money.isPositive(amount), "Amount must be positive");
        require(amount.stripTrailingZeros().scale() <= Money.SCALE, "Amount can have at most 2 decimals");
    }

    private static void requireText(String value, String field) {
        require(value != null && !value.isBlank(), field + " is required");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }
}
