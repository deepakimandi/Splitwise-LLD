package com.lld.splitwise.strategy;

import com.lld.splitwise.exception.ValidationException;
import com.lld.splitwise.model.Money;
import com.lld.splitwise.model.Split;
import com.lld.splitwise.model.SplitInput;
import com.lld.splitwise.model.SplitType;

import java.math.BigDecimal;
import java.util.List;

public class ExactSplitStrategy extends AbstractSplitStrategy {

    @Override
    public SplitType type() { return SplitType.EXACT; }

    @Override
    protected void validate(BigDecimal total, List<SplitInput> inputs) {
        requirePositiveValues(inputs, "Exact amount");
        for (SplitInput in : inputs) {
            if (in.value().stripTrailingZeros().scale() > Money.SCALE) {
                throw new ValidationException("Exact amount has more than 2 decimals: " + in.value());
            }
        }
        BigDecimal sum = inputs.stream().map(SplitInput::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(total) != 0) {
            throw new ValidationException("Exact amounts add up to " + sum + " but total is " + total);
        }
    }

    @Override
    protected List<Split> compute(BigDecimal total, List<SplitInput> inputs) {
        return inputs.stream().map(in -> new Split(in.user(), Money.normalize(in.value()))).toList();
    }
}
