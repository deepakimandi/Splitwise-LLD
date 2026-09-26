package com.lld.splitwise.controller;

import com.lld.splitwise.dto.AddMemberRequest;
import com.lld.splitwise.dto.CreateGroupRequest;
import com.lld.splitwise.service.GroupService;

public class GroupController extends BaseController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /** POST /groups  -> returns the new groupId */
    public ApiResponse<String> createGroup(CreateGroupRequest request) {
        return handle(() -> groupService.createGroup(request).getGroupId());
    }

    /** POST /groups/{groupId}/members */
    public ApiResponse<Void> addMember(AddMemberRequest request) {
        return handle(() -> {
            groupService.addMember(request);
            return null;
        });
    }

    /** DELETE /groups/{groupId}/members/{userId} */
    public ApiResponse<Void> removeMember(String requesterId, String groupId, String userId) {
        return handle(() -> {
            groupService.removeMember(requesterId, groupId, userId);
            return null;
        });
    }
}
