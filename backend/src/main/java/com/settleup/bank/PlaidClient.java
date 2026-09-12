package com.settleup.bank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlaidClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String secret;
    private final String webhookUrl;

    public PlaidClient(
            ObjectMapper objectMapper,
            @Value("${plaid.client-id:}") String clientId,
            @Value("${plaid.secret:}") String secret,
            @Value("${plaid.environment:sandbox}") String environment,
            @Value("${plaid.webhook-url:}") String webhookUrl
    ) {
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.secret = secret;
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.builder().baseUrl(baseUrl(environment)).build();
    }

    public LinkToken createLinkToken(String userId) {
        Map<String, Object> body = authenticatedBody();
        body.put("client_name", "SettleUp");
        body.put("language", "en");
        body.put("country_codes", List.of("US"));
        body.put("products", List.of("transactions"));
        body.put("user", Map.of("client_user_id", userId));
        body.put("transactions", Map.of("days_requested", 90));
        if (!webhookUrl.isBlank()) {
            body.put("webhook", webhookUrl);
        }
        JsonNode response = post("/link/token/create", body);
        return new LinkToken(requiredText(response, "link_token"), Instant.parse(requiredText(response, "expiration")));
    }

    public ExchangedItem exchangePublicToken(String publicToken) {
        Map<String, Object> body = authenticatedBody();
        body.put("public_token", publicToken);
        JsonNode response = post("/item/public_token/exchange", body);
        return new ExchangedItem(requiredText(response, "access_token"), requiredText(response, "item_id"));
    }

    public List<AccountData> getAccounts(String accessToken) {
        Map<String, Object> body = authenticatedBody();
        body.put("access_token", accessToken);
        JsonNode response = post("/accounts/get", body);
        List<AccountData> accounts = new ArrayList<>();
        response.path("accounts").forEach(account -> accounts.add(new AccountData(
                requiredText(account, "account_id"),
                requiredText(account, "name"),
                optionalText(account, "official_name"),
                optionalText(account, "mask"),
                requiredText(account, "type"),
                optionalText(account, "subtype"))));
        return accounts;
    }

    public SyncPage syncTransactions(String accessToken, String cursor) {
        Map<String, Object> body = authenticatedBody();
        body.put("access_token", accessToken);
        if (cursor != null && !cursor.isBlank()) {
            body.put("cursor", cursor);
        }
        body.put("count", 500);
        JsonNode response = post("/transactions/sync", body);
        return new SyncPage(
                transactions(response.path("added")),
                transactions(response.path("modified")),
                removedIds(response.path("removed")),
                requiredTextAllowBlank(response, "next_cursor"),
                response.path("has_more").asBoolean(false));
    }

    public void removeItem(String accessToken) {
        Map<String, Object> body = authenticatedBody();
        body.put("access_token", accessToken);
        post("/item/remove", body);
    }

    public JsonNode getWebhookVerificationKey(String keyId) {
        Map<String, Object> body = authenticatedBody();
        body.put("key_id", keyId);
        return post("/webhook_verification_key/get", body).path("key");
    }

    private List<TransactionData> transactions(JsonNode nodes) {
        List<TransactionData> transactions = new ArrayList<>();
        nodes.forEach(node -> transactions.add(new TransactionData(
                requiredText(node, "transaction_id"),
                requiredText(node, "account_id"),
                requiredText(node, "name"),
                optionalText(node, "merchant_name"),
                toCents(node.path("amount").decimalValue()),
                currency(node),
                optionalDate(node, "authorized_date"),
                LocalDate.parse(requiredText(node, "date")),
                node.path("pending").asBoolean(false))));
        return transactions;
    }

    private List<String> removedIds(JsonNode nodes) {
        List<String> ids = new ArrayList<>();
        nodes.forEach(node -> ids.add(requiredText(node, "transaction_id")));
        return ids;
    }

    private String currency(JsonNode transaction) {
        String currency = optionalText(transaction, "iso_currency_code");
        return currency == null ? "USD" : currency;
    }

    private LocalDate optionalDate(JsonNode node, String field) {
        String value = optionalText(node, field);
        return value == null ? null : LocalDate.parse(value);
    }

    private long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private JsonNode post(String path, Map<String, Object> body) {
        requireConfigured();
        try {
            return restClient.post()
                    .uri(path)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw apiException(exception);
        } catch (RestClientException exception) {
            throw new PlaidApiException("PLAID_UNAVAILABLE", "Plaid is currently unavailable", exception);
        }
    }

    private PlaidApiException apiException(RestClientResponseException exception) {
        try {
            JsonNode error = objectMapper.readTree(exception.getResponseBodyAsString());
            String code = optionalText(error, "error_code");
            String message = optionalText(error, "error_message");
            return new PlaidApiException(
                    code == null ? "PLAID_ERROR" : code,
                    message == null ? "Plaid request failed" : message,
                    exception);
        } catch (Exception parseException) {
            return new PlaidApiException("PLAID_ERROR", "Plaid request failed", exception);
        }
    }

    private Map<String, Object> authenticatedBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_id", clientId);
        body.put("secret", secret);
        return body;
    }

    private void requireConfigured() {
        if (clientId.isBlank() || secret.isBlank()) {
            throw new PlaidApiException("PLAID_NOT_CONFIGURED", "Plaid credentials are not configured", null);
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null) {
            throw new PlaidApiException("INVALID_PLAID_RESPONSE", "Plaid response omitted " + field, null);
        }
        return value;
    }

    private String requiredTextAllowBlank(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual()) {
            throw new PlaidApiException("INVALID_PLAID_RESPONSE", "Plaid response omitted " + field, null);
        }
        return value.asText();
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private String baseUrl(String environment) {
        return switch (environment.toLowerCase()) {
            case "sandbox" -> "https://sandbox.plaid.com";
            case "development" -> "https://development.plaid.com";
            case "production" -> "https://production.plaid.com";
            default -> throw new IllegalStateException("Unsupported PLAID_ENV: " + environment);
        };
    }

    public record LinkToken(String value, Instant expiration) {
    }

    public record ExchangedItem(String accessToken, String itemId) {
    }

    public record AccountData(
            String accountId,
            String name,
            String officialName,
            String mask,
            String type,
            String subtype
    ) {
    }

    public record TransactionData(
            String transactionId,
            String accountId,
            String name,
            String merchantName,
            long amountCents,
            String isoCurrencyCode,
            LocalDate authorizedDate,
            LocalDate postedDate,
            boolean pending
    ) {
    }

    public record SyncPage(
            List<TransactionData> added,
            List<TransactionData> modified,
            List<String> removedTransactionIds,
            String nextCursor,
            boolean hasMore
    ) {
    }
}
