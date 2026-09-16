package com.settleup.classification;

import com.settleup.bank.BankAccount;
import com.settleup.bank.BankAccountRepository;
import com.settleup.bank.BankConnection;
import com.settleup.bank.BankConnectionRepository;
import com.settleup.bank.BankTransaction;
import com.settleup.bank.BankTransactionRepository;
import com.settleup.bank.PlaidClient;
import com.settleup.expense.ExpenseRepository;
import com.settleup.group.Group;
import com.settleup.support.AbstractIntegrationTest;
import com.settleup.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuggestionFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BankConnectionRepository connections;

    @Autowired
    private BankAccountRepository accounts;

    @Autowired
    private BankTransactionRepository transactions;

    @Autowired
    private ExpenseSuggestionRepository suggestions;

    @Autowired
    private ExpenseRepository expenses;

    @Test
    void confirmCreatesExpenseAndRejectRecordsFeedback() throws Exception {
        User alex = data.user("Alex");
        User blair = data.user("Blair");
        Group group = data.group("Dinner club", alex, blair);
        BankConnection connection = connections.saveAndFlush(
                new BankConnection(alex, "item-1", "ins-1", "Test Bank", "encrypted"));
        BankAccount account = accounts.saveAndFlush(
                new BankAccount(connection, "account-1", "Checking", null, "1234", "depository", "checking"));
        ExpenseSuggestion confirm = suggestion(account, group, "txn-confirm", "Corner Cafe", 6400);
        ExpenseSuggestion reject = suggestion(account, group, "txn-reject", "Wrong Merchant", 1800);
        String token = login(alex.getEmail());

        mockMvc.perform(get("/suggestions").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(post("/suggestions/{id}/confirm", confirm.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.expense.amountCents").value(6400));

        mockMvc.perform(post("/suggestions/{id}/reject", reject.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        org.assertj.core.api.Assertions.assertThat(expenses.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(suggestions.findById(reject.getId()).orElseThrow().getStatus())
                .isEqualTo(ExpenseSuggestionStatus.REJECTED);
    }

    private ExpenseSuggestion suggestion(
            BankAccount account,
            Group group,
            String transactionId,
            String merchant,
            long cents
    ) {
        BankTransaction transaction = transactions.saveAndFlush(new BankTransaction(account,
                new PlaidClient.TransactionData(
                        transactionId, account.getPlaidAccountId(), merchant, merchant, cents, "USD",
                        "FOOD_AND_DRINK", LocalDate.now(), LocalDate.now(), false)));
        return suggestions.saveAndFlush(new ExpenseSuggestion(
                transaction, group, 0.85, List.of("This merchant is common for this group")));
    }
}
