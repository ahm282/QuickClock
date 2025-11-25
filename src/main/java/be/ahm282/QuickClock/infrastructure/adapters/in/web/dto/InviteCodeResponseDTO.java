package be.ahm282.QuickClock.infrastructure.adapters.in.web.dto;

import java.time.Instant;

public record InviteCodeResponseDTO(
        String code,
        Instant expiresAt,
        Long createdByUserId,
        boolean isUsed,
        boolean isRevoked,
        Instant createdAt
) {}
