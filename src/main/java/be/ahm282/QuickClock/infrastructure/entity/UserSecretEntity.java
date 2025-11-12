package be.ahm282.QuickClock.infrastructure.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_secrets")
public class UserSecretEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String secret;

    public UserSecretEntity() {}

    public UserSecretEntity(Long userId, String secret) {
        this.userId = userId;
        this.secret = secret;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long id) {
        this.userId = id;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
