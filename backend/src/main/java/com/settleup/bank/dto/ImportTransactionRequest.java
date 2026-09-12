package com.settleup.bank.dto;

import com.settleup.expense.SplitStrategy;
import com.settleup.expense.dto.ExpenseSplitRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ImportTransactionRequest(
        @NotNull UUID groupId,
        @Size(max = 255) String description,
        @NotNull SplitStrategy splitStrategy,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid ExpenseSplitRequest> splits
) {
}
