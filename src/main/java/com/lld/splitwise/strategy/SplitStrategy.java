package com.lld.splitwise.strategy;

import com.lld.splitwise.model.Split;
import com.lld.splitwise.model.SplitInput;
import com.lld.splitwise.model.SplitType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Strategy pattern: one implementation per way of dividing money.
 * Contract: returned splits always add up EXACTLY to {@code total} (no lost paise).
 */
public interface SplitStrategy {
    SplitType type();

    List<Split> split(BigDecimal total, List<SplitInput> inputs);
}
