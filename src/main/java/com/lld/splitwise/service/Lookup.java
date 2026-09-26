package com.lld.splitwise.service;

import com.lld.splitwise.exception.NotFoundException;
import com.lld.splitwise.exception.ForbiddenException;
import com.lld.splitwise.model.Group;
import com.lld.splitwise.model.User;
import com.lld.splitwise.repository.GroupRepository;
import com.lld.splitwise.repository.UserRepository;

/** Small shared helper for "load or 404" and "must be a member" checks used by all services. */
class Lookup {
    private final UserRepository users;
    private final GroupRepository groups;

    Lookup(UserRepository users, GroupRepository groups) {
        this.users = users;
        this.groups = groups;
    }

    User user(String userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }

    Group group(String groupId) {
        return groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group", groupId));
    }

    /** Authorization: only members may read or change a group. */
    void requireMember(Group group, String userId) {
        if (!group.hasMember(userId)) {
            throw new ForbiddenException("User " + userId + " is not a member of " + group.getName());
        }
    }
}
