package com.settleup.classification;

import com.settleup.bank.BankTransaction;
import com.settleup.bank.BankTransactionRepository;
import com.settleup.expense.ExpenseRepository;
import com.settleup.expense.Expense;
import com.settleup.common.Money;
import com.settleup.group.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationEngineTest {

    @Mock
    private BankTransactionRepository transactionRepository;
    @Mock
    private ExpenseSuggestionRepository suggestionRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private BankTransaction transaction;
    @Mock
    private Group group;
    @Mock
    private Expense expense;

    private ClassificationEngine engine;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        when(group.getId()).thenReturn(groupId);
        when(transaction.getMerchantName()).thenReturn("Luna Pizza");
        engine = new ClassificationEngine(List.of(
                new MerchantHistoryRule(transactionRepository, suggestionRepository),
                new CategoryRule(transactionRepository),
                new AmountThresholdRule(expenseRepository)));
    }

    @Test
    void confirmingMerchantOnceRaisesConfidenceAboveSuggestionFloor() {
        when(expenseRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId)).thenReturn(List.of());
        when(transactionRepository.existsConfirmedMerchantForGroup(groupId, "Luna Pizza"))
                .thenReturn(false, true);

        ClassificationEngine.Result before = engine.classify(transaction, group);
        ClassificationEngine.Result after = engine.classify(transaction, group);

        assertThat(before.confidence()).isZero();
        assertThat(after.confidence()).isEqualTo(0.55);
        assertThat(after.reasons()).singleElement().asString().contains("previously shared");
    }

    @Test
    void rejectionSuppressesFutureSuggestionsForMerchantAndGroup() {
        when(suggestionRepository.existsRejectedMerchantForGroup(groupId, "Luna Pizza")).thenReturn(true);

        ClassificationEngine.Result result = engine.classify(transaction, group);

        assertThat(result.suppressed()).isTrue();
        assertThat(result.confidence()).isZero();
        assertThat(result.reasons()).singleElement().asString().contains("previously said");
    }

    @Test
    void unrelatedTransactionProducesNoConfidenceOrReason() {
        when(expenseRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId)).thenReturn(List.of());
        ClassificationEngine.Result result = engine.classify(transaction, group);

        assertThat(result.suppressed()).isFalse();
        assertThat(result.confidence()).isZero();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void matchingCategoryAndTypicalAmountCanSurfaceANewMerchant() {
        when(transaction.getCategory()).thenReturn("FOOD_AND_DRINK");
        when(transaction.getAmountCents()).thenReturn(8_000L);
        when(transactionRepository.existsConfirmedCategoryForGroup(groupId, "FOOD_AND_DRINK")).thenReturn(true);
        when(expense.getAmount()).thenReturn(new Money(8_000));
        when(expenseRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId)).thenReturn(List.of(expense));

        ClassificationEngine.Result result = engine.classify(transaction, group);

        assertThat(result.confidence()).isEqualTo(0.45);
        assertThat(result.reasons()).hasSize(2);
    }
}
