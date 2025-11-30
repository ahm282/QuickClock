package be.ahm282.QuickClock.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for refresh tokens with token family support.
 * Enables strict token rotation and reuse detection.
 */
public class RefreshToken {
    private String jti;
    private UUID parentId;
    private UUID rootFamilyId;
    private Long userId;
    private boolean revoked;
    private boolean used;
    private Instant issuedAt;
    private Instant expiresAt;

    public RefreshToken() {}

    public RefreshToken(String jti, UUID parentId, UUID rootFamilyId, Long userId, boolean revoked,
                        boolean used, Instant issuedAt, Instant expiresAt) {
        this.jti = jti;
        this.parentId = parentId;
        this.rootFamilyId = rootFamilyId;
        this.userId = userId;
        this.revoked = revoked;
        this.used = used;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    // Getters and setters
    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public UUID getRootFamilyId() {
        return rootFamilyId;
    }

    public void setRootFamilyId(UUID rootFamilyId) {
        this.rootFamilyId = rootFamilyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}

