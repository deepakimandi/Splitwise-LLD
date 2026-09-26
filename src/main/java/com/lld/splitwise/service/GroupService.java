package com.lld.splitwise.service;

import com.lld.splitwise.dto.AddMemberRequest;
import com.lld.splitwise.dto.CreateGroupRequest;
import com.lld.splitwise.model.Group;
import com.lld.splitwise.model.User;
import com.lld.splitwise.observer.EventPublisher;
import com.lld.splitwise.observer.EventType;
import com.lld.splitwise.observer.SplitwiseEvent;
import com.lld.splitwise.repository.GroupRepository;
import com.lld.splitwise.repository.UserRepository;

import java.util.Set;

public class GroupService {
    private final GroupRepository groups;
    private final Lookup lookup;
    private final IdGenerator ids;
    private final EventPublisher events;

    public GroupService(GroupRepository groups, UserRepository users, IdGenerator ids, EventPublisher events) {
        this.groups = groups;
        this.lookup = new Lookup(users, groups);
        this.ids = ids;
        this.events = events;
    }

    public Group createGroup(CreateGroupRequest request) {
        User creator = lookup.user(request.requesterId());
        Group group = new Group(ids.next("G"), request.name(), creator);
        if (request.memberIds() != null) {
            request.memberIds().forEach(id -> group.addMember(lookup.user(id)));  // 404 on unknown user
        }
        groups.save(group);
        return group;
    }

    public void addMember(AddMemberRequest request) {
        Group group = lookup.group(request.groupId());
        lookup.requireMember(group, request.requesterId());
        User actor = lookup.user(request.requesterId());
        User newMember = lookup.user(request.userId());
        group.addMember(newMember);
        events.publish(SplitwiseEvent.of(EventType.MEMBER_ADDED, group.getGroupId(), actor,
                group.getMembers(), actor + " added " + newMember + " to '" + group.getName() + "'"));
    }

    /** Throws ConflictException if the member still owes / is owed money in this group. */
    public void removeMember(String requesterId, String groupId, String userId) {
        Group group = lookup.group(groupId);
        lookup.requireMember(group, requesterId);
        User actor = lookup.user(requesterId);
        User member = lookup.user(userId);
        group.removeMember(member);
        events.publish(SplitwiseEvent.of(EventType.MEMBER_REMOVED, groupId, actor,
                Set.of(member), actor + " removed " + member + " from '" + group.getName() + "'"));
    }

    public Group get(String groupId) {
        return lookup.group(groupId);
    }
}
