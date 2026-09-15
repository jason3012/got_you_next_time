package com.settleup.classification;

import com.settleup.auth.AuthenticatedUser;
import com.settleup.classification.dto.ConfirmSuggestionRequest;
import com.settleup.classification.dto.ConfirmedSuggestionResponse;
import com.settleup.classification.dto.ExpenseSuggestionResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/suggestions")
public class ExpenseSuggestionController {

    private final ClassificationService classificationService;

    public ExpenseSuggestionController(ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @GetMapping
    public List<ExpenseSuggestionResponse> pending(@AuthenticationPrincipal Jwt jwt) {
        return classificationService.listPending(AuthenticatedUser.id(jwt));
    }

    @PostMapping("/{suggestionId}/confirm")
    public ConfirmedSuggestionResponse confirm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID suggestionId,
            @Valid @RequestBody(required = false) ConfirmSuggestionRequest request
    ) {
        return classificationService.confirm(AuthenticatedUser.id(jwt), suggestionId, request);
    }

    @PostMapping("/{suggestionId}/reject")
    public ExpenseSuggestionResponse reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID suggestionId
    ) {
        return classificationService.reject(AuthenticatedUser.id(jwt), suggestionId);
    }
}
