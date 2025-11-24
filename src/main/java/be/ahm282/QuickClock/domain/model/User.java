package be.ahm282.QuickClock.domain.model;

public class User {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String secret;
    private final Role role;

    public User(Long id, String username, String passwordHash, String secret, Role role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.secret = secret;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSecret() {
        return secret;
    }

    public Role getRole() {
        return role;
    }
}
