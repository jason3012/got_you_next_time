package com.settleup.classification;

import com.settleup.bank.BankAccount;
import com.settleup.bank.BankConnection;
import com.settleup.bank.BankTransaction;
import com.settleup.classification.dto.ConfirmedSuggestionResponse;
import com.settleup.expense.Expense;
import com.settleup.expense.ExpenseRepository;
import com.settleup.expense.ExpenseService;
import com.settleup.expense.SplitStrategy;
import com.settleup.expense.dto.CreateExpenseRequest;
import com.settleup.expense.dto.ExpenseResponse;
import com.settleup.group.Group;
import com.settleup.group.GroupMember;
import com.settleup.group.GroupMemberRepository;
import com.settleup.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationServiceTest {

    @Mock private ClassificationEngine classificationEngine;
    @Mock private ExpenseSuggestionRepository suggestionRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private ExpenseService expenseService;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private BankTransaction transaction;
    @Mock private BankAccount account;
    @Mock private BankConnection connection;
    @Mock private User owner;
    @Mock private Group group;
    @Mock private GroupMember membership;

    private ClassificationService service;
    private UUID userId;
    private UUID groupId;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        service = new ClassificationService(
                classificationEngine,
                suggestionRepository,
                groupMemberRepository,
                expenseService,
                expenseRepository,
                0.45);
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
    }

    @Test
    void belowFloorTransactionStaysSilent() {
        prepareClassifiableTransaction();
        when(groupMemberRepository.findAllByUserId(userId)).thenReturn(List.of(membership));
        when(membership.getGroup()).thenReturn(group);
        when(classificationEngine.classify(transaction, group))
                .thenReturn(new ClassificationEngine.Result(0.44, List.of("Weak match"), false));
        when(suggestionRepository.findByTransactionIdAndCandidateGroupId(transactionId, groupId))
                .thenReturn(Optional.empty());

        service.classify(transaction);

        verify(suggestionRepository, never()).save(any());
    }

    @Test
    void confirmationCreatesAnEqualExpenseAndLinksTheTransaction() {
        UUID suggestionId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        User first = org.mockito.Mockito.mock(User.class);
        User second = org.mockito.Mockito.mock(User.class);
        GroupMember firstMembership = org.mockito.Mockito.mock(GroupMember.class);
        GroupMember secondMembership = org.mockito.Mockito.mock(GroupMember.class);
        Expense expense = org.mockito.Mockito.mock(Expense.class);
        ExpenseSuggestion suggestion = new ExpenseSuggestion(transaction, group, 0.78, List.of("Known merchant"));
        setField(suggestion, "id", suggestionId);

        when(suggestionRepository.findByIdAndTransactionAccountConnectionUserId(suggestionId, userId))
                .thenReturn(Optional.of(suggestion));
        when(transaction.getId()).thenReturn(transactionId);
        when(transaction.getMerchantName()).thenReturn("Luna Pizza");
        when(transaction.getAmountCents()).thenReturn(8_420L);
        when(transaction.getIsoCurrencyCode()).thenReturn("USD");
        when(transaction.getPostedDate()).thenReturn(LocalDate.of(2026, 9, 15));
        when(group.getId()).thenReturn(groupId);
        when(group.getName()).thenReturn("Sunday crew");
        when(first.getId()).thenReturn(UUID.randomUUID());
        when(second.getId()).thenReturn(UUID.randomUUID());
        when(firstMembership.getUser()).thenReturn(first);
        when(secondMembership.getUser()).thenReturn(second);
        when(groupMemberRepository.findAllByGroupIdOrderByCreatedAtAsc(groupId))
                .thenReturn(List.of(firstMembership, secondMembership));
        ExpenseResponse expenseResponse = new ExpenseResponse(
                expenseId, groupId, userId, userId, "Luna Pizza", 8_420,
                SplitStrategy.EQUAL, List.of(), null);
        when(expenseService.create(any(), any(), any())).thenReturn(expenseResponse);
        when(expenseRepository.getReferenceById(expenseId)).thenReturn(expense);
        when(suggestionRepository.findAllByTransactionIdAndStatus(
                transactionId, ExpenseSuggestionStatus.PENDING)).thenReturn(List.of());

        ConfirmedSuggestionResponse result = service.confirm(userId, suggestionId, null);

        ArgumentCaptor<CreateExpenseRequest> request = ArgumentCaptor.forClass(CreateExpenseRequest.class);
        verify(expenseService).create(org.mockito.ArgumentMatchers.eq(groupId),
                org.mockito.ArgumentMatchers.eq(userId), request.capture());
        assertThat(request.getValue().splitStrategy()).isEqualTo(SplitStrategy.EQUAL);
        assertThat(request.getValue().splits()).hasSize(2);
        verify(transaction).linkExpense(expense);
        assertThat(result.suggestion().status()).isEqualTo(ExpenseSuggestionStatus.CONFIRMED);
        assertThat(result.expense()).isEqualTo(expenseResponse);
    }

    private void prepareClassifiableTransaction() {
        when(transaction.isPending()).thenReturn(false);
        when(transaction.isRemoved()).thenReturn(false);
        when(transaction.getExpense()).thenReturn(null);
        when(transaction.getAmountCents()).thenReturn(5_000L);
        when(transaction.getId()).thenReturn(transactionId);
        when(transaction.getAccount()).thenReturn(account);
        when(account.getConnection()).thenReturn(connection);
        when(connection.getUser()).thenReturn(owner);
        when(owner.getId()).thenReturn(userId);
        when(group.getId()).thenReturn(groupId);
    }

    private void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
