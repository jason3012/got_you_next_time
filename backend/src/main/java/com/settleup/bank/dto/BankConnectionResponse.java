package com.settleup.bank.dto;

import com.settleup.bank.BankConnectionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BankConnectionResponse(
        UUID id,
        String institutionName,
        BankConnectionStatus status,
        String errorCode,
        Instant lastSyncedAt,
        Instant createdAt,
        List<BankAccountResponse> accounts
) {
}
