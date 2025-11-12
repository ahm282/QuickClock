package be.ahm282.QuickClock.infrastructure.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // <--- ADD THIS LINE
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false, unique = true)
    private String secret;

    public UserEntity() {}
    public UserEntity(String username, String passwordHash, String secret) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.secret = secret;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
