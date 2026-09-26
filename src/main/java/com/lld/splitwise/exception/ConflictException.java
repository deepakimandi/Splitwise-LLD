package com.lld.splitwise.exception;

/**
 * The request conflicts with current state (HTTP 409), e.g.
 * - the same idempotency key is still being processed,
 * - the same idempotency key was reused with a different payload,
 * - removing a member who still has an unsettled balance.
 */
public class ConflictException extends SplitwiseException {
    public ConflictException(String message) {
        super(message);
    }
}
