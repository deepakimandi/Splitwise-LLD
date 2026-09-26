package com.lld.splitwise.exception;

/** Base unchecked exception for the domain. Controllers map subclasses to HTTP codes. */
public class SplitwiseException extends RuntimeException {
    public SplitwiseException(String message) {
        super(message);
    }
}
