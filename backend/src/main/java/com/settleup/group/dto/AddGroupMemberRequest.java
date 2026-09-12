package com.settleup.group.dto;

import com.settleup.group.GroupMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddGroupMemberRequest(
        @NotBlank @Email String email,
        @NotNull GroupMemberRole role
) {
}
