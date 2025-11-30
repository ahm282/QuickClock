package be.ahm282.QuickClock.domain.model;

import java.time.Instant;
import java.util.UUID;

public class InviteCode {
    private final UUID id;
    private final String code;
    private final Instant expiresAt;
    private final boolean used;
    private final boolean revoked;
    private final Long revokedByUserId;
    private final Instant revokedAt;
    private final Long usedByUserId;
    private final Long createdByUserId;
    private final Instant createdAt;

    public InviteCode(UUID id, String code, Instant expiresAt, boolean used, boolean revoked, Long revokedByUserId,
                      Instant revokedAt, Long usedByUserId, Long createdByUserId, Instant createdAt) {
        this.id = id;
        this.code = code;
        this.expiresAt = expiresAt;
        this.used = used;
        this.revoked = revoked;
        this.revokedByUserId = revokedByUserId;
        this.revokedAt = revokedAt;
        this.usedByUserId = usedByUserId;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }

    // ... Getters ...
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public boolean isRevoked() { return revoked; }
    public Long getUsedByUserId() { return usedByUserId; }
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isCreatedBy(Long userId) { return createdByUserId != null && createdByUserId.equals(userId); }
    public Long getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getRevokedByUserId() { return revokedByUserId; }
    public Instant getRevokedAt() { return revokedAt; }

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