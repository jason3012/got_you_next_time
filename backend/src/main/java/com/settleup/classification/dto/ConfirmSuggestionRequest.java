package com.settleup.classification.dto;

import com.settleup.expense.SplitStrategy;
import com.settleup.expense.dto.ExpenseSplitRequest;
import jakarta.validation.Valid;

import java.util.List;

public record ConfirmSuggestionRequest(
        SplitStrategy splitStrategy,
        List<@Valid ExpenseSplitRequest> splits
) {
    public ConfirmSuggestionRequest {
        splits = splits == null ? List.of() : List.copyOf(splits);
    }
}
