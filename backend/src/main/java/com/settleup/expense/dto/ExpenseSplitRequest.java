package com.settleup.expense.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ExpenseSplitRequest(
        @NotNull UUID userId,
        Long amountCents,
        Integer percentage
) {
}
