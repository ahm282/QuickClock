package be.ahm282.QuickClock.infrastructure.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity for storing refresh tokens with family tracking.
 * Supports strict token rotation and reuse detection.
 *
 * UUID Storage Strategy:
 * - Uses BINARY(16) for MySQL/MariaDB for optimal storage (16 bytes vs 36 bytes for VARCHAR)
 * - H2 database (dev/test) handles this automatically
 * - PostgreSQL: Consider using 'uuid' type instead (requires dialect configuration)
 * - If portability is critical, use VARCHAR(36) with @Type(UUIDCharType.class)
 *
 * Current configuration optimized for: MySQL/MariaDB + H2
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_jti", columnList = "jti"),
        @Index(name = "idx_refresh_user", columnList = "user_id"),
        @Index(name = "idx_refresh_family", columnList = "root_family_id"),
        @Index(name = "idx_refresh_parent", columnList = "parent_id")
})
public class RefreshTokenEntity {
    @Id
    @Column(name = "jti", nullable = false, unique = true, length = 100)
    private String jti;

    /**
     * Parent token ID (previous token in the rotation chain).
     * BINARY(16) stores UUID efficiently in MySQL/MariaDB (16 bytes).
     * For PostgreSQL, consider: @Column(columnDefinition = "uuid")
     * For maximum portability: @Column(length = 36) + VARCHAR
     */
    @Column(name = "parent_id", columnDefinition = "BINARY(16)")
    private UUID parentId;

    /**
     * Root family ID (identifies the login session/token family).
     * BINARY(16) stores UUID efficiently in MySQL/MariaDB (16 bytes).
     * For PostgreSQL, consider: @Column(columnDefinition = "uuid")
     * For maximum portability: @Column(length = 36) + VARCHAR
     */
    @Column(name = "root_family_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID rootFamilyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RefreshTokenEntity() {}

    public RefreshTokenEntity(String jti, UUID parentId, UUID rootFamilyId, Long userId, String username,
                            boolean revoked, boolean used, Instant issuedAt, Instant expiresAt) {
        this.jti = jti;
        this.parentId = parentId;
        this.rootFamilyId = rootFamilyId;
        this.userId = userId;
        this.username = username;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

