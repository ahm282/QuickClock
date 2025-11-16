package be.ahm282.QuickClock.infrastructure.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "invalidates_tokens")
public class InvalidatedTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expiry_time", nullable = false)
    private Instant expiryTime; // Token's original expiry

    public InvalidatedTokenEntity() {}

    public InvalidatedTokenEntity(String jti, Long userId, Instant expiryTime) {
        this.jti = jti;
        this.userId = userId;
        this.expiryTime = expiryTime;
    }

    public String getJti() {
        return jti;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }
}
