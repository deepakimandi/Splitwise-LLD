package com.lld.splitwise.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A real "settle up" payment: {@code from} paid {@code to} some money, inside a group.
 * It is recorded in the ledger just like an expense, so it reduces what {@code from} owes.
 */
public record Payment(String paymentId, String groupId, User from, User to, BigDecimal amount, Instant createdAt) {
}
