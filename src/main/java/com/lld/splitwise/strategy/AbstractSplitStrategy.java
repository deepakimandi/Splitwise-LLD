package com.lld.splitwise.strategy;

import com.lld.splitwise.exception.ValidationException;
import com.lld.splitwise.model.Money;
import com.lld.splitwise.model.Split;
import com.lld.splitwise.model.SplitInput;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Template Method pattern: the fixed skeleton (common checks -> type-specific checks -> compute)
 * lives here; subclasses fill in only validate() and compute().
 * Also hosts the shared rounding helper so every strategy handles paise the same way.
 */
public abstract class AbstractSplitStrategy implements SplitStrategy {

    @Override
    public final List<Split> split(BigDecimal total, List<SplitInput> inputs) {
        if (!Money.isPositive(total)) {
            throw new ValidationException("Total must be positive");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new ValidationException("At least one participant is required");
        }
        Set<String> seen = new HashSet<>();
        for (SplitInput in : inputs) {
            if (!seen.add(in.user().getUserId())) {
                throw new ValidationException("Duplicate participant: " + in.user());
            }
        }
        validate(total, inputs);
        return compute(total, inputs);
    }

    protected abstract void validate(BigDecimal total, List<SplitInput> inputs);

    protected abstract List<Split> compute(BigDecimal total, List<SplitInput> inputs);

    /**
     * Divide {@code total} in proportion to {@code weights} without losing a paisa.
     * Each share is rounded DOWN to 2 decimals, then the leftover paise (always fewer than
     * the number of participants) are handed out one by one, in input order.
     * Example: 100.00 / 3  ->  33.34, 33.33, 33.33  (sum = 100.00, not 99.99)
     */
    protected static List<Split> allocate(BigDecimal total, List<SplitInput> inputs, List<BigDecimal> weights) {
        BigDecimal weightSum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            BigDecimal share = total.multiply(w).divide(weightSum, Money.SCALE, RoundingMode.DOWN);
            shares.add(share);
            allocated = allocated.add(share);
        }
        BigDecimal remainder = total.subtract(allocated);
        for (int i = 0; remainder.signum() > 0; i = (i + 1) % shares.size()) {
            shares.set(i, shares.get(i).add(Money.ONE_UNIT));
            remainder = remainder.subtract(Money.ONE_UNIT);
        }
        List<Split> result = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            result.add(new Split(inputs.get(i).user(), shares.get(i)));
        }
        return result;
    }

    protected static void requirePositiveValues(List<SplitInput> inputs, String what) {
        for (SplitInput in : inputs) {
            if (!Money.isPositive(in.value())) {
                throw new ValidationException(what + " for " + in.user() + " must be positive");
            }
        }
    }
}
