package com.settleup.settlement.dto;

import java.util.UUID;

public record TransferResponse(
        UUID fromUserId,
        UUID toUserId,
        long amountCents
) {
}
