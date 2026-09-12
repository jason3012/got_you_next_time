package com.settleup.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExchangePublicTokenRequest(
        @NotBlank String publicToken,
        @Size(max = 255) String institutionId,
        @Size(max = 255) String institutionName
) {
}
