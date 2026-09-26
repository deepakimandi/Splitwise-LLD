package com.lld.splitwise.exception;

/** Caller is not allowed to act on this resource, e.g. not a member of the group (HTTP 403). */
public class ForbiddenException extends SplitwiseException {
    public ForbiddenException(String message) {
        super(message);
    }
}
