package be.ahm282.QuickClock.infrastructure.adapters.in.web.dto;

import java.time.Instant;

public record QrScanStatusDTO(
        String tokenId,
        Long userId,
        String direction, // "IN" or "OUT"
        Instant clockedAt
) {
}
