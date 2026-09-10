package com.settleup.expense;

import com.settleup.expense.dto.CreateExpenseRequest;
import com.settleup.expense.dto.ExpenseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(
            @PathVariable UUID groupId,
            @RequestParam UUID userId,
            @Valid @RequestBody CreateExpenseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.create(groupId, userId, request));
    }

    @GetMapping
    public List<ExpenseResponse> list(@PathVariable UUID groupId, @RequestParam UUID userId) {
        return expenseService.list(groupId, userId);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID groupId,
            @PathVariable UUID expenseId,
            @RequestParam UUID userId
    ) {
        expenseService.delete(groupId, userId, expenseId);
        return ResponseEntity.noContent().build();
    }
}
