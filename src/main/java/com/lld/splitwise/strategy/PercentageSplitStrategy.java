package com.lld.splitwise.strategy;

import com.lld.splitwise.exception.ValidationException;
import com.lld.splitwise.model.Split;
import com.lld.splitwise.model.SplitInput;
import com.lld.splitwise.model.SplitType;

import java.math.BigDecimal;
import java.util.List;

public class PercentageSplitStrategy extends AbstractSplitStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public SplitType type() { return SplitType.PERCENTAGE; }

    @Override
    protected void validate(BigDecimal total, List<SplitInput> inputs) {
        requirePositiveValues(inputs, "Percentage");
        BigDecimal sum = inputs.stream().map(SplitInput::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(HUNDRED) != 0) {
            throw new ValidationException("Percentages must add up to 100, got " + sum);
        }
    }

    @Override
    protected List<Split> compute(BigDecimal total, List<SplitInput> inputs) {
        // percentages are just weights that sum to 100
        return allocate(total, inputs, inputs.stream().map(SplitInput::value).toList());
    }
}
