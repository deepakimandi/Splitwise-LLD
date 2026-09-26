package com.lld.splitwise.idempotency;

import java.time.Instant;

/**
 * What we remember about one idempotency key.
 * requestHash lets us detect a client bug: same key reused with a DIFFERENT payload.
 */
public record IdempotencyRecord(IdempotencyStatus status, String requestHash, Object response, Instant createdAt) {

    static IdempotencyRecord inProgress(String requestHash) {
        return new IdempotencyRecord(IdempotencyStatus.IN_PROGRESS, requestHash, null, Instant.now());
    }

    IdempotencyRecord complete(Object response) {
        return new IdempotencyRecord(IdempotencyStatus.COMPLETED, requestHash, response, createdAt);
    }
}
