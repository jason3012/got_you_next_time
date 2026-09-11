package com.settleup.settlement;

import com.settleup.auth.AuthenticatedUser;
import com.settleup.settlement.dto.BalanceResponse;
import com.settleup.settlement.dto.RecordSettlementRequest;
import com.settleup.settlement.dto.SettlementResponse;
import com.settleup.settlement.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups/{groupId}")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/balances")
    public List<BalanceResponse> balances(@PathVariable UUID groupId, @AuthenticationPrincipal Jwt jwt) {
        return settlementService.balances(groupId, AuthenticatedUser.id(jwt));
    }

    @GetMapping("/settle-up")
    public List<TransferResponse> plan(@PathVariable UUID groupId, @AuthenticationPrincipal Jwt jwt) {
        return settlementService.plan(groupId, AuthenticatedUser.id(jwt));
    }

    @PostMapping("/settlements")
    public ResponseEntity<SettlementResponse> record(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecordSettlementRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(settlementService.record(groupId, AuthenticatedUser.id(jwt), request));
    }
}
