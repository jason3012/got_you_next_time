package com.settleup.expense.dto;

import java.util.UUID;

public record ExpenseSplitResponse(
        UUID userId,
        long shareCents
) {
}
