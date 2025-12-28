package be.ahm282.QuickClock.domain.model;

import java.time.Instant;
import java.util.UUID;

public record InviteCode(UUID id, String code, Instant expiresAt, boolean used, boolean revoked, Long revokedByUserId,
                         Instant revokedAt, Long usedByUserId, Long createdByUserId, Instant createdAt) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isCreatedBy(Long userId) {
        return createdByUserId != null && createdByUserId.equals(userId);
    }

    public InviteCode markAsUsed(Long userId) {
        return new InviteCode(
                id, code, expiresAt,
                true,
                false,
                revokedByUserId,
                revokedAt,
                userId,
                createdByUserId,
                createdAt
        );
    }

    public InviteCode revoke(Long revokedByUserId, Instant revokedAt) {
        return new InviteCode(
                id, code, expiresAt,
                used,
                true,
                revokedByUserId,
                revokedAt,
                usedByUserId,
                createdByUserId,
                createdAt
        );
    }
}