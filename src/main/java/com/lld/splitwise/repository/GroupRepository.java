package com.lld.splitwise.repository;

import com.lld.splitwise.model.Group;

import java.util.List;
import java.util.Optional;

public interface GroupRepository {
    void save(Group group);

    Optional<Group> findById(String groupId);

    /** All groups a user belongs to (for the user's overall balance summary). */
    List<Group> findByMember(String userId);
}
