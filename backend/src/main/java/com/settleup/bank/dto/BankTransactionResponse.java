package com.settleup.bank.dto;

import java.time.LocalDate;
import java.util.UUID;

public record BankTransactionResponse(
        UUID id,
        UUID accountId,
        String accountName,
        String name,
        String merchantName,
        long amountCents,
        String isoCurrencyCode,
        LocalDate authorizedDate,
        LocalDate postedDate,
        boolean pending,
        UUID expenseId
) {
}
