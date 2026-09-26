package com.lld.splitwise.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The user's home screen: overall net position plus the breakdown per group.
 * Derived on read from the group ledgers, never stored separately (one source of truth).
 */
public record UserBalanceSummary(String userId, BigDecimal overallNet, Map<String, BigDecimal> netByGroupName) {
}
