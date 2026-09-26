package com.lld.splitwise.exception;

/** Referenced user / group / expense does not exist (HTTP 404). */
public class NotFoundException extends SplitwiseException {
    public NotFoundException(String entity, String id) {
        super(entity + " not found: " + id);
    }
}
