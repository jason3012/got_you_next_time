package com.settleup.expense.dto;

import com.settleup.expense.SplitStrategy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID groupId,
        UUID payerId,
        UUID createdBy,
        String description,
        long amountCents,
        SplitStrategy splitStrategy,
        List<ExpenseSplitResponse> splits,
        Instant createdAt
) {
}
