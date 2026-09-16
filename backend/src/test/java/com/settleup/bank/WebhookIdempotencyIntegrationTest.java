package com.settleup.bank;

import com.settleup.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebhookIdempotencyIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private PlaidClient plaidClient;

    @MockitoBean
    private PlaidWebhookVerifier verifier;

    @Autowired
    private BankTransactionRepository transactions;

    @Test
    void replayingWebhookDoesNotDuplicateTransactions() throws Exception {
        String token = register("alex@example.com", "Alex");
        PlaidClient.TransactionData transaction = new PlaidClient.TransactionData(
                "txn-1", "account-1", "Dinner", "Corner Cafe", 4200, "USD",
                "FOOD_AND_DRINK", LocalDate.now(), LocalDate.now(), false);

        when(plaidClient.exchangePublicToken("public-token"))
                .thenReturn(new PlaidClient.ExchangedItem("access-token", "item-1"));
        when(plaidClient.getAccounts("access-token"))
                .thenReturn(List.of(new PlaidClient.AccountData(
                        "account-1", "Checking", null, "1234", "depository", "checking")));
        when(plaidClient.syncTransactions("access-token", null))
                .thenReturn(new PlaidClient.SyncPage(List.of(), List.of(), List.of(), "cursor-0", false));
        when(plaidClient.syncTransactions("access-token", "cursor-0"))
                .thenReturn(new PlaidClient.SyncPage(List.of(transaction), List.of(), List.of(), "cursor-1", false));
        when(plaidClient.syncTransactions("access-token", "cursor-1"))
                .thenReturn(new PlaidClient.SyncPage(List.of(transaction), List.of(), List.of(), "cursor-2", false));
        when(verifier.verify(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/bank/connections")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"publicToken":"public-token","institutionName":"Test Bank"}
                                """))
                .andExpect(status().isCreated());

        String webhook = """
                {"webhook_type":"TRANSACTIONS","webhook_code":"SYNC_UPDATES_AVAILABLE","item_id":"item-1"}
                """;
        for (int replay = 0; replay < 2; replay++) {
            mockMvc.perform(post("/plaid/webhook")
                            .header("Plaid-Verification", "valid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhook))
                    .andExpect(status().isOk());
        }

        org.assertj.core.api.Assertions.assertThat(transactions.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(
                transactions.findByPlaidTransactionId("txn-1").orElseThrow().getMerchantName())
                .isEqualTo("Corner Cafe");
    }
}
