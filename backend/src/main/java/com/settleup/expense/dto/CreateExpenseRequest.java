package com.settleup.expense.dto;

import com.settleup.expense.SplitStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateExpenseRequest(
        @NotBlank @Size(max = 255) String description,
        @Positive long amountCents,
        @NotNull UUID payerId,
        @NotNull SplitStrategy splitStrategy,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid ExpenseSplitRequest> splits
) {
}
