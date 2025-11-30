package be.ahm282.QuickClock.domain.model;

import java.time.Instant;

public class InvalidatedToken {
    private String jti;
    private Long userId;
    private Instant expiryTime;

    public InvalidatedToken() {}

    public InvalidatedToken(String jti, Long userId, Instant expiryTime) {
        this.jti = jti;
        this.userId = userId;
        this.expiryTime = expiryTime;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Instant expiryTime) {
        this.expiryTime = expiryTime;
    }
}
