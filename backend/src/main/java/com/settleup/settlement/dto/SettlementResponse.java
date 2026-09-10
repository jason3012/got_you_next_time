package com.settleup.settlement.dto;

import java.time.Instant;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID groupId,
        UUID fromUserId,
        UUID toUserId,
        long amountCents,
        Instant createdAt
) {
}
