package com.settleup.group.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String currencyCode,
        UUID createdBy,
        List<GroupMemberResponse> members,
        Instant createdAt,
        Instant updatedAt
) {
}
