package be.ahm282.QuickClock.domain.model;

import java.time.Instant;

public record TokenValidationResult(
        long userId,
        String purpose,
        String kioskId,
        Instant issuedAt,
        Instant expiresAt,
        String tokenId
) {}
