package be.ahm282.QuickClock.infrastructure.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "invalidated_tokens", indexes = {
        @Index(name = "idx_invalidated_jti", columnList = "jti"),
        @Index(name = "idx_invalidated_user", columnList = "user_id"),
        @Index(name = "idx_invalidated_expiry", columnList = "expiry_time")
})
public class InvalidatedTokenEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, length = 100)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expiry_time", nullable = false)
    private Instant expiryTime;

    public InvalidatedTokenEntity() {}

    public InvalidatedTokenEntity(String jti, Long userId, Instant expiryTime) {
        this.jti = jti;
        this.userId = userId;
        this.expiryTime = expiryTime;
    }

    public Long getId() {
        return id;
    }

    public String getJti() { return jti; }

    public Long getUserId() { return userId; }

    public Instant getExpiryTime() { return expiryTime; }
}
