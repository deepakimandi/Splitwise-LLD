package com.lld.splitwise.strategy;

import com.lld.splitwise.model.Split;
import com.lld.splitwise.model.SplitInput;
import com.lld.splitwise.model.SplitType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class EqualSplitStrategy extends AbstractSplitStrategy {

    @Override
    public SplitType type() { return SplitType.EQUAL; }

    @Override
    protected void validate(BigDecimal total, List<SplitInput> inputs) {
        // nothing beyond the common checks; SplitInput.value is ignored
    }

    @Override
    protected List<Split> compute(BigDecimal total, List<SplitInput> inputs) {
        return allocate(total, inputs, Collections.nCopies(inputs.size(), BigDecimal.ONE));
    }
}
