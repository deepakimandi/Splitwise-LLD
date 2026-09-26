package com.lld.splitwise.service;

import java.util.concurrent.atomic.AtomicLong;

/** Readable, thread-safe ids for the demo (U1, G1, E1...). Production: UUID / Snowflake ids. */
public class IdGenerator {
    private final AtomicLong counter = new AtomicLong();

    public String next(String prefix) {
        return prefix + counter.incrementAndGet();
    }
}
