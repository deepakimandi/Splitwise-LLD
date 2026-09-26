package com.lld.splitwise.model;

/** Supported ways of dividing an expense. Adding a type = new enum value + new strategy class. */
public enum SplitType {
    EQUAL,       // everyone pays the same (remainder paise distributed)
    EXACT,       // caller gives each person's exact amount
    PERCENTAGE,  // caller gives percentages that sum to 100
    SHARES       // caller gives shares/weights, e.g. 2:1:1 (added to show Open/Closed in action)
}
