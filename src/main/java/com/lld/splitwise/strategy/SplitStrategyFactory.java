package com.lld.splitwise.strategy;

import com.lld.splitwise.exception.ValidationException;
import com.lld.splitwise.model.SplitType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory backed by a registry instead of a switch statement.
 * Strategies are stateless, so one shared instance per type is enough (Flyweight-like reuse).
 * New split type = new class + register it here (or inject the list through DI).
 */
public class SplitStrategyFactory {

    private final Map<SplitType, SplitStrategy> registry = new EnumMap<>(SplitType.class);

    public SplitStrategyFactory(List<SplitStrategy> strategies) {
        strategies.forEach(s -> registry.put(s.type(), s));
    }

    public static SplitStrategyFactory withDefaults() {
        return new SplitStrategyFactory(List.of(
                new EqualSplitStrategy(),
                new ExactSplitStrategy(),
                new PercentageSplitStrategy(),
                new SharesSplitStrategy()));
    }

    public SplitStrategy get(SplitType type) {
        SplitStrategy strategy = registry.get(type);
        if (strategy == null) {
            throw new ValidationException("Unsupported split type: " + type);
        }
        return strategy;
    }
}
