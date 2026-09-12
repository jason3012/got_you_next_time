package com.settleup.bank.dto;

import java.time.Instant;

public record SyncResponse(int added, int modified, int removed, Instant syncedAt) {
}
