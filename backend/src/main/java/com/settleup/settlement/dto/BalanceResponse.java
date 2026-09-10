package com.settleup.settlement.dto;

import java.util.UUID;

public record BalanceResponse(
        UUID userId,
        String displayName,
        long balanceCents
) {
}
