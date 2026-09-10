package com.settleup.settlement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record RecordSettlementRequest(
        @NotNull UUID fromUserId,
        @NotNull UUID toUserId,
        @Positive long amountCents
) {
}
