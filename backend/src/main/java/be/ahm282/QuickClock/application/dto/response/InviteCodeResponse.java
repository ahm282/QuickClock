package be.ahm282.QuickClock.application.dto.response;

import java.time.Instant;

public record InviteCodeResponse(
        String code,
        Instant expiresAt,
        Long createdByUserId,
        boolean isUsed,
        boolean isRevoked,
        Instant createdAt
) {}
