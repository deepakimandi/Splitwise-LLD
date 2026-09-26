package com.lld.splitwise.model;

import java.math.BigDecimal;

/**
 * A suggested transfer produced by debt simplification ("from should pay to amount").
 * Nothing changes in the ledger until the user actually records it as a Payment.
 */
public record Transaction(User from, User to, BigDecimal amount) {
    @Override
    public String toString() {
        return from + " pays " + to + " " + amount;
    }
}
