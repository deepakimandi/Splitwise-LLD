package com.lld.splitwise.exception;

/** Bad input: invalid amount, splits that don't add up, non-member, etc. (HTTP 400). */
public class ValidationException extends SplitwiseException {
    public ValidationException(String message) {
        super(message);
    }
}
