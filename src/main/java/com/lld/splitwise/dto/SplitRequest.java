package com.lld.splitwise.dto;

import java.math.BigDecimal;

/** value = null for EQUAL, rupees for EXACT, percent for PERCENTAGE, weight for SHARES. */
public record SplitRequest(String userId, BigDecimal value) {
    public static SplitRequest of(String userId) {
        return new SplitRequest(userId, null);
    }

    public static SplitRequest of(String userId, String value) {
        return new SplitRequest(userId, new BigDecimal(value));
    }
}
