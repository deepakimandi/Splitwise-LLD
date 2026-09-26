package com.lld.splitwise.idempotency;

public enum IdempotencyStatus {
    IN_PROGRESS,   // first request is still running
    COMPLETED      // response is stored and can be replayed
}
