package com.lld.splitwise.model;

import java.math.BigDecimal;

/**
 * One participant's share of an expense, as computed by a SplitStrategy.
 * Immutable value object: strategies return new Splits instead of mutating inputs.
 */
public record Split(User user, BigDecimal amount) {
}
