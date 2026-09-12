package com.settleup.bank.dto;

import java.time.Instant;

public record LinkTokenResponse(String linkToken, Instant expiration) {
}
