package com.settleup.group.dto;

import com.settleup.group.GroupMemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddGroupMemberRequest(
        @NotNull UUID userId,
        @NotNull GroupMemberRole role
) {
}
