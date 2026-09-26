package com.lld.splitwise.model;

import java.math.BigDecimal;

/**
 * Raw input for a participant before the strategy runs.
 * value means: ignored for EQUAL, rupees for EXACT, percent for PERCENTAGE, weight for SHARES.
 * One uniform input type means no casting (the old PercentageSplit cast is gone).
 */
public record SplitInput(User user, BigDecimal value) {
}
