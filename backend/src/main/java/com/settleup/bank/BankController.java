package com.settleup.bank;

import com.settleup.auth.AuthenticatedUser;
import com.settleup.bank.dto.BankConnectionResponse;
import com.settleup.bank.dto.BankTransactionResponse;
import com.settleup.bank.dto.ExchangePublicTokenRequest;
import com.settleup.bank.dto.ImportTransactionRequest;
import com.settleup.bank.dto.LinkTokenResponse;
import com.settleup.bank.dto.SyncResponse;
import com.settleup.expense.dto.ExpenseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bank")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @PostMapping("/link-token")
    public LinkTokenResponse createLinkToken(@AuthenticationPrincipal Jwt jwt) {
        return bankService.createLinkToken(AuthenticatedUser.id(jwt));
    }

    @PostMapping("/connections")
    public ResponseEntity<BankConnectionResponse> exchange(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExchangePublicTokenRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bankService.exchange(AuthenticatedUser.id(jwt), request));
    }

    @GetMapping("/connections")
    public List<BankConnectionResponse> connections(@AuthenticationPrincipal Jwt jwt) {
        return bankService.listConnections(AuthenticatedUser.id(jwt));
    }

    @PostMapping("/connections/{connectionId}/sync")
    public SyncResponse sync(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID connectionId) {
        return bankService.sync(AuthenticatedUser.id(jwt), connectionId);
    }

    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID connectionId) {
        bankService.disconnect(AuthenticatedUser.id(jwt), connectionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transactions")
    public List<BankTransactionResponse> transactions(@AuthenticationPrincipal Jwt jwt) {
        return bankService.listTransactions(AuthenticatedUser.id(jwt));
    }

    @PostMapping("/transactions/{transactionId}/import")
    public ResponseEntity<ExpenseResponse> importTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID transactionId,
            @Valid @RequestBody ImportTransactionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bankService.importTransaction(AuthenticatedUser.id(jwt), transactionId, request));
    }
}
