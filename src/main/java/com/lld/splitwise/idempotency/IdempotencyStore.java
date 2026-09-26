package com.lld.splitwise.idempotency;

import com.lld.splitwise.exception.ConflictException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Makes "create" operations safe to retry.
 *
 * Flow for key K:
 *   - putIfAbsent(K, IN_PROGRESS)  is ONE atomic step, so only one request can win the key.
 *     (A separate exists() + save() would let two concurrent retries both pass the check.)
 *   - winner runs the operation, then stores COMPLETED + response.
 *   - a later retry with K gets the stored response (no second expense).
 *   - a concurrent retry while IN_PROGRESS gets 409 and should retry after a short delay.
 *   - if the operation fails, K is removed so the client can safely retry.
 *
 * Keys are scoped per caller ("userId:key") so two users can't collide.
 * In production this is a DB table / Redis with a unique constraint on the key and a TTL (e.g. 24h).
 */
public class IdempotencyStore {

    private final ConcurrentHashMap<String, IdempotencyRecord> records = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T execute(String scopedKey, String requestHash, Supplier<T> operation) {
        IdempotencyRecord existing = records.putIfAbsent(scopedKey, IdempotencyRecord.inProgress(requestHash));

        if (existing != null) {
            if (!existing.requestHash().equals(requestHash)) {
                throw new ConflictException("Idempotency key reused with a different request");
            }
            if (existing.status() == IdempotencyStatus.IN_PROGRESS) {
                throw new ConflictException("Request with this idempotency key is still in progress");
            }
            return (T) existing.response();                 // replay: same result, no side effects
        }

        try {
            T response = operation.get();
            records.computeIfPresent(scopedKey, (k, r) -> r.complete(response));
            return response;
        } catch (RuntimeException e) {
            records.remove(scopedKey);                      // failed => allow a clean retry
            throw e;
        }
    }

    public int size() {
        return records.size();
    }
}
