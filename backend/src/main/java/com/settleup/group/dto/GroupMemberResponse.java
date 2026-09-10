package com.settleup.group.dto;

import com.settleup.group.GroupMemberRole;

import java.util.UUID;

public record GroupMemberResponse(
        UUID userId,
        String displayName,
        String email,
        GroupMemberRole role
) {
}
