package com.settleup.settlement;

import com.settleup.settlement.dto.BalanceResponse;
import com.settleup.settlement.dto.RecordSettlementRequest;
import com.settleup.settlement.dto.SettlementResponse;
import com.settleup.settlement.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/groups/{groupId}")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/balances")
    public List<BalanceResponse> balances(@PathVariable UUID groupId, @RequestParam UUID userId) {
        return settlementService.balances(groupId, userId);
    }

    @GetMapping("/settle-up")
    public List<TransferResponse> plan(@PathVariable UUID groupId, @RequestParam UUID userId) {
        return settlementService.plan(groupId, userId);
    }

    @PostMapping("/settlements")
    public ResponseEntity<SettlementResponse> record(
            @PathVariable UUID groupId,
            @RequestParam UUID userId,
            @Valid @RequestBody RecordSettlementRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(settlementService.record(groupId, userId, request));
    }
}
