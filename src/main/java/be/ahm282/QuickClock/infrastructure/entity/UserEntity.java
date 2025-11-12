package be.ahm282.QuickClock.infrastructure.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private Long id;
    private String username;
    private String secret;

    public UserEntity() {}
    public UserEntity(Long id, String username, String secret) {
        this.id = id;
        this.username = username;
        this.secret = secret;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
