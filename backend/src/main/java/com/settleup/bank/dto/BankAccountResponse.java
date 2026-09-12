package com.settleup.bank.dto;

import java.util.UUID;

public record BankAccountResponse(
        UUID id,
        String name,
        String officialName,
        String mask,
        String type,
        String subtype
) {
}
