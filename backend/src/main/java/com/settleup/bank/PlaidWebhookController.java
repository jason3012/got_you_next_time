package com.settleup.bank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleup.common.exception.UnauthorizedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plaid")
public class PlaidWebhookController {

    private final BankService bankService;
    private final PlaidWebhookVerifier verifier;
    private final ObjectMapper objectMapper;

    public PlaidWebhookController(
            BankService bankService,
            PlaidWebhookVerifier verifier,
            ObjectMapper objectMapper
    ) {
        this.bankService = bankService;
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader("Plaid-Verification") String verification,
            @RequestBody String rawBody
    ) throws Exception {
        if (!verifier.verify(verification, rawBody)) {
            throw new UnauthorizedException("Invalid Plaid webhook signature");
        }
        JsonNode payload = objectMapper.readTree(rawBody);
        if ("TRANSACTIONS".equals(payload.path("webhook_type").asText())
                && "SYNC_UPDATES_AVAILABLE".equals(payload.path("webhook_code").asText())) {
            String itemId = payload.path("item_id").asText();
            if (!itemId.isBlank()) {
                bankService.syncWebhook(itemId);
            }
        }
        return ResponseEntity.ok().build();
    }
}
