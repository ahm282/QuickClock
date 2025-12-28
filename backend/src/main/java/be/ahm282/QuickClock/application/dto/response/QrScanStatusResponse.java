package be.ahm282.QuickClock.application.dto.response;

import java.time.Instant;

public record QrScanStatusResponse(
        String tokenId,
        Long userId,
        String direction, // "IN" or "OUT"
        Instant clockedAt
) {
}
