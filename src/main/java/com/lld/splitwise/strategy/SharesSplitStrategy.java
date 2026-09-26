package com.lld.splitwise.strategy;

import com.lld.splitwise.model.Split;
import com.lld.splitwise.model.SplitInput;
import com.lld.splitwise.model.SplitType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Split by shares, e.g. a couple = 2 shares, a single person = 1 share.
 * Added without touching any existing strategy or the service: Open/Closed Principle.
 */
public class SharesSplitStrategy extends AbstractSplitStrategy {

    @Override
    public SplitType type() { return SplitType.SHARES; }

    @Override
    protected void validate(BigDecimal total, List<SplitInput> inputs) {
        requirePositiveValues(inputs, "Share");
    }

    @Override
    protected List<Split> compute(BigDecimal total, List<SplitInput> inputs) {
        return allocate(total, inputs, inputs.stream().map(SplitInput::value).toList());
    }
}
