package com.lld.splitwise.observer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Builds the per-group "Activity" tab / audit trail. Added without touching any service. */
public class ActivityFeedListener implements EventListener {

    private final Map<String, List<SplitwiseEvent>> feedByGroup = new ConcurrentHashMap<>();

    @Override
    public void onEvent(SplitwiseEvent event) {
        feedByGroup.computeIfAbsent(event.groupId(), g -> new CopyOnWriteArrayList<>()).add(event);
    }

    public List<SplitwiseEvent> feed(String groupId) {
        return List.copyOf(feedByGroup.getOrDefault(groupId, List.of()));
    }
}
