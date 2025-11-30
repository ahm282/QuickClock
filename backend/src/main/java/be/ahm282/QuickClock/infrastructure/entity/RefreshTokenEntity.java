package be.ahm282.QuickClock.infrastructure.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_jti", columnList = "jti"),
        @Index(name = "idx_refresh_user", columnList = "user_id"),
        @Index(name = "idx_refresh_family", columnList = "root_family_id"),
        @Index(name = "idx_refresh_parent", columnList = "parent_id"),
        @Index(name = "idx_refresh_expires", columnList = "expires_at")
})
public class RefreshTokenEntity {
    @Id
    @Column(name = "jti", nullable = false, length = 100)
    private String jti;

    @Column(name = "parent_id", columnDefinition = "uuid")
    private UUID parentId;

    @Column(name = "root_family_id", nullable = false, columnDefinition = "uuid")
    private UUID rootFamilyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RefreshTokenEntity() {}

    public RefreshTokenEntity(String jti, UUID parentId, UUID rootFamilyId, Long userId, boolean revoked,
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

