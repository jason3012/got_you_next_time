package com.settleup.classification.dto;

import com.settleup.classification.ExpenseSuggestionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseSuggestionResponse(
        UUID id,
        UUID transactionId,
        UUID groupId,
        String groupName,
        String merchantName,
        long amountCents,
        String isoCurrencyCode,
        String category,
        LocalDate postedDate,
        double confidence,
        List<String> reasons,
        ExpenseSuggestionStatus status,
        Instant createdAt
) {
}
