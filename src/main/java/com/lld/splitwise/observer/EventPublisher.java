package com.lld.splitwise.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Subject in the Observer pattern.
 *
 * - Services only know this publisher, not email/push/feed code (loose coupling, SRP).
 * - CopyOnWriteArrayList: safe to iterate while listeners subscribe (reads >> writes).
 * - Each listener is isolated with try/catch: a failing email sender must not
 *   roll back an expense or stop other listeners.
 * - The Executor decides sync vs async. Pass Runnable::run for tests, a thread pool in prod.
 *   Real systems publish to a queue (Kafka/SQS) via the Outbox pattern, so an event is never
 *   lost if the process crashes right after the DB commit.
 */
public class EventPublisher {

    private final List<EventListener> listeners = new CopyOnWriteArrayList<>();
    private final Executor executor;

    public EventPublisher(Executor executor) {
        this.executor = executor;
    }

    public static EventPublisher synchronous() {
        return new EventPublisher(Runnable::run);
    }

    public void subscribe(EventListener listener)   { listeners.add(listener); }
    public void unsubscribe(EventListener listener) { listeners.remove(listener); }

    /** Call AFTER the ledger update has succeeded (and outside any lock). */
    public void publish(SplitwiseEvent event) {
        for (EventListener listener : listeners) {
            executor.execute(() -> {
                try {
                    listener.onEvent(event);
                } catch (RuntimeException e) {
                    System.err.println("Listener " + listener.getClass().getSimpleName() + " failed: " + e.getMessage());
                }
            });
        }
    }
}
