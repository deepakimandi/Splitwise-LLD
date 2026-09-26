package com.lld.splitwise.concurrency;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Fine-grained, deadlock-free locking over a set of keys (user ids).
 *
 * Deadlock avoidance = ordered locking:
 *   1. de-duplicate the keys (payer is usually also a participant),
 *   2. sort them (TreeSet gives one global order),
 *   3. acquire in that order, release in reverse order in a finally block.
 * Because every thread acquires in the same order, a circular wait cannot form.
 *
 * Note: the lock map grows with the number of keys. At scale, use lock striping
 * (a fixed array of N locks indexed by hash) and sort by stripe index instead.
 */
public class LockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private ReentrantLock lockFor(String key) {
        // computeIfAbsent is atomic: two threads can never create two different locks for one key.
        // (putIfAbsent + get also works, but allocates a lock on every call.)
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    public void withLocks(Collection<String> keys, Runnable action) {
        withLocks(keys, () -> {
            action.run();
            return null;
        });
    }

    public <T> T withLocks(Collection<String> keys, Supplier<T> action) {
        TreeSet<String> ordered = new TreeSet<>(keys);   // dedupe + global ordering
        Deque<ReentrantLock> acquired = new ArrayDeque<>();
        try {
            for (String key : ordered) {
                ReentrantLock lock = lockFor(key);
                lock.lock();
                acquired.push(lock);                      // only locks we really hold get released
            }
            return action.get();                          // critical section
        } finally {
            while (!acquired.isEmpty()) {
                acquired.pop().unlock();                  // reverse order
            }
        }
    }
}
