package com.lld.splitwise.repository.inmemory;

import com.lld.splitwise.model.Group;
import com.lld.splitwise.repository.GroupRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGroupRepository implements GroupRepository {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    @Override
    public void save(Group group) { groups.put(group.getGroupId(), group); }

    @Override
    public Optional<Group> findById(String groupId) { return Optional.ofNullable(groups.get(groupId)); }

    @Override
    public List<Group> findByMember(String userId) {
        // a real DB would use an index on group_members(user_id)
        return groups.values().stream().filter(g -> g.hasMember(userId)).toList();
    }
}
