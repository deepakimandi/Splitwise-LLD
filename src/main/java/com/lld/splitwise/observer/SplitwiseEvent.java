package com.lld.splitwise.observer;

import com.lld.splitwise.model.User;

import java.time.Instant;
import java.util.Set;

/** Immutable event. Listeners get everything they need, so they never call back into services. */
public record SplitwiseEvent(EventType type, String groupId, User actor, Set<User> affectedUsers,
                             String message, Instant occurredAt) {

    public static SplitwiseEvent of(EventType type, String groupId, User actor, Set<User> affected, String message) {
        return new SplitwiseEvent(type, groupId, actor, Set.copyOf(affected), message, Instant.now());
    }
}
