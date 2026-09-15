package com.settleup.classification.dto;

import com.settleup.expense.dto.ExpenseResponse;

public record ConfirmedSuggestionResponse(
        ExpenseSuggestionResponse suggestion,
        ExpenseResponse expense
) {
}
