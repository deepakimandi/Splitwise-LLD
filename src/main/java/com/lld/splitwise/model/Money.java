package com.lld.splitwise.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money helpers. All amounts are BigDecimal with scale 2 (rupees.paise).
 * Never use double for money: 0.1 + 0.2 != 0.3 in floating point.
 */
public final class Money {
    public static final int SCALE = 2;
    /** Smallest unit we can move: 1 paisa. */
    public static final BigDecimal ONE_UNIT = new BigDecimal("0.01");

    private Money() {}

    public static BigDecimal of(String amount) {
        return normalize(new BigDecimal(amount));
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    public static boolean isZero(BigDecimal amount) {
        return amount.signum() == 0;
    }
}
