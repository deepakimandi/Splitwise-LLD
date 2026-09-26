package com.lld.splitwise.dto;

import java.util.List;

/** DTOs are the API contract. They are separate from domain objects so either can change independently. */
public record CreateGroupRequest(String requesterId, String name, List<String> memberIds) {
}
