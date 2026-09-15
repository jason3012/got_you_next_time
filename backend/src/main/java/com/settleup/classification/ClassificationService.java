package com.settleup.classification;

import com.settleup.bank.BankTransaction;
import com.settleup.classification.dto.ConfirmSuggestionRequest;
import com.settleup.classification.dto.ConfirmedSuggestionResponse;
import com.settleup.classification.dto.ExpenseSuggestionResponse;
import com.settleup.common.exception.ConflictException;
import com.settleup.common.exception.NotFoundException;
import com.settleup.common.exception.ValidationException;
import com.settleup.expense.Expense;
import com.settleup.expense.ExpenseRepository;
import com.settleup.expense.ExpenseService;
import com.settleup.expense.SplitStrategy;
import com.settleup.expense.dto.CreateExpenseRequest;
import com.settleup.expense.dto.ExpenseResponse;
import com.settleup.expense.dto.ExpenseSplitRequest;
import com.settleup.group.Group;
import com.settleup.group.GroupMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClassificationService {

    private final ClassificationEngine classificationEngine;
    private final ExpenseSuggestionRepository suggestionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final double confidenceFloor;

    public ClassificationService(
            ClassificationEngine classificationEngine,
            ExpenseSuggestionRepository suggestionRepository,
            GroupMemberRepository groupMemberRepository,
            ExpenseService expenseService,
            ExpenseRepository expenseRepository,
            @Value("${classification.confidence-floor:0.45}") double confidenceFloor
    ) {
        if (confidenceFloor < 0 || confidenceFloor > 1) {
            throw new IllegalArgumentException("classification.confidence-floor must be between 0 and 1");
        }
        this.classificationEngine = classificationEngine;
        this.suggestionRepository = suggestionRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
        this.confidenceFloor = confidenceFloor;
    }

    @Transactional
    public void classify(BankTransaction transaction) {
        if (transaction.isPending() || transaction.isRemoved() || transaction.getExpense() != null
                || transaction.getAmountCents() <= 0) {
            clearPending(transaction.getId());
            return;
        }

        List<Group> groups = groupMemberRepository.findAllByUserId(
                        transaction.getAccount().getConnection().getUser().getId()).stream()
                .map(membership -> membership.getGroup())
                .distinct()
                .toList();

        for (Group group : groups) {
            ClassificationEngine.Result result = classificationEngine.classify(transaction, group);
            ExpenseSuggestion existing = suggestionRepository
                    .findByTransactionIdAndCandidateGroupId(transaction.getId(), group.getId())
                    .orElse(null);

            if (result.suppressed() || result.confidence() < confidenceFloor || result.reasons().isEmpty()) {
                if (existing != null && existing.getStatus() == ExpenseSuggestionStatus.PENDING) {
                    suggestionRepository.delete(existing);
                }
                continue;
            }

            if (existing == null) {
                suggestionRepository.save(new ExpenseSuggestion(
                        transaction, group, result.confidence(), result.reasons()));
            } else {
                existing.update(result.confidence(), result.reasons());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ExpenseSuggestionResponse> listPending(UUID userId) {
        return suggestionRepository
                .findAllByTransactionAccountConnectionUserIdAndStatusOrderByConfidenceDescCreatedAtDesc(
                        userId, ExpenseSuggestionStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ConfirmedSuggestionResponse confirm(
            UUID userId,
            UUID suggestionId,
            ConfirmSuggestionRequest request
    ) {
        ExpenseSuggestion suggestion = requireSuggestion(suggestionId, userId);
        requirePending(suggestion);
        BankTransaction transaction = suggestion.getTransaction();
        if (transaction.isRemoved() || transaction.isPending() || transaction.getExpense() != null) {
            throw new ConflictException("This transaction is no longer available to share");
        }

        SplitStrategy strategy = request == null || request.splitStrategy() == null
                ? SplitStrategy.EQUAL
                : request.splitStrategy();
        List<ExpenseSplitRequest> splits = request == null ? List.of() : request.splits();
        if (splits.isEmpty()) {
            if (strategy != SplitStrategy.EQUAL) {
                throw new ValidationException("Exact and percentage confirmations require split details");
            }
            splits = groupMemberRepository.findAllByGroupIdOrderByCreatedAtAsc(
                            suggestion.getCandidateGroup().getId()).stream()
                    .map(member -> new ExpenseSplitRequest(member.getUser().getId(), null, null))
                    .toList();
        }

        ExpenseResponse expenseResponse = expenseService.create(
                suggestion.getCandidateGroup().getId(),
                userId,
                new CreateExpenseRequest(
                        merchant(transaction),
                        transaction.getAmountCents(),
                        userId,
                        strategy,
                        splits));
        Expense expense = expenseRepository.getReferenceById(expenseResponse.id());
        transaction.linkExpense(expense);
        suggestion.confirm();
        suggestionRepository.findAllByTransactionIdAndStatus(transaction.getId(), ExpenseSuggestionStatus.PENDING)
                .stream()
                .filter(other -> !other.getId().equals(suggestion.getId()))
                .forEach(suggestionRepository::delete);

        return new ConfirmedSuggestionResponse(toResponse(suggestion), expenseResponse);
    }

    @Transactional
    public ExpenseSuggestionResponse reject(UUID userId, UUID suggestionId) {
        ExpenseSuggestion suggestion = requireSuggestion(suggestionId, userId);
        requirePending(suggestion);
        suggestion.reject();
        return toResponse(suggestion);
    }

    @Transactional
    public void clearPending(UUID transactionId) {
        suggestionRepository.deleteAllByTransactionIdAndStatus(transactionId, ExpenseSuggestionStatus.PENDING);
    }

    private ExpenseSuggestion requireSuggestion(UUID suggestionId, UUID userId) {
        return suggestionRepository.findByIdAndTransactionAccountConnectionUserId(suggestionId, userId)
                .orElseThrow(() -> new NotFoundException("Expense suggestion was not found"));
    }

    private void requirePending(ExpenseSuggestion suggestion) {
        if (suggestion.getStatus() != ExpenseSuggestionStatus.PENDING) {
            throw new ConflictException("This expense suggestion has already been reviewed");
        }
    }

    private ExpenseSuggestionResponse toResponse(ExpenseSuggestion suggestion) {
        BankTransaction transaction = suggestion.getTransaction();
        return new ExpenseSuggestionResponse(
                suggestion.getId(),
                transaction.getId(),
                suggestion.getCandidateGroup().getId(),
                suggestion.getCandidateGroup().getName(),
                merchant(transaction),
                transaction.getAmountCents(),
                transaction.getIsoCurrencyCode(),
                transaction.getCategory(),
                transaction.getPostedDate(),
                suggestion.getConfidence(),
                suggestion.getReasons(),
                suggestion.getStatus(),
                suggestion.getCreatedAt());
    }

    private String merchant(BankTransaction transaction) {
        return transaction.getMerchantName() == null ? transaction.getName() : transaction.getMerchantName();
    }
}
