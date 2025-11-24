package be.ahm282.QuickClock.domain.model;

import java.time.Instant;
import java.util.UUID;

public class InviteCode {
    private final UUID id;
    private final String code;
    private final Instant expiresAt;
    private final boolean used;
    private final Long usedByUserId;

    public InviteCode(UUID id, String code, Instant expiresAt, boolean used, Long usedByUserId) {
        this.id = id;
        this.code = code;
        this.expiresAt = expiresAt;
        this.used = used;
        this.usedByUserId = usedByUserId;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public Long getUsedByUserId() {
        return usedByUserId;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isCreatedBy(Long userId) {
        return usedByUserId.equals(userId);
    }

    public InviteCode markAsUsed(Long userId) {
        return new InviteCode(id, code, expiresAt, true, userId);
    }
}
