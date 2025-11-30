package be.ahm282.QuickClock.infrastructure.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite_codes", indexes = {
        @Index(name = "idx_invite_code", columnList = "code")
})
public class InviteCodeEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "used_by_user_id")
    private Long usedByUserId;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "revoked_by_user_id")
    private Long revokedByUserId;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public InviteCodeEntity() {}

    public InviteCodeEntity(String code, Instant expiresAt) {
        this.code = code;
        this.expiresAt = expiresAt;
    }

    // --- GETTERS AND SETTERS ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Long getUsedByUserId() {
        return usedByUserId;
    }

    public void setUsedByUserId(Long usedByUserId) {
        this.usedByUserId = usedByUserId;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Long getRevokedByUserId() {
        return revokedByUserId;
    }

    public void setRevokedByUserId(Long revokedByUserId) {
        this.revokedByUserId = revokedByUserId;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}