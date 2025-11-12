package be.ahm282.QuickClock.domain.model;

public class User {

    private final Long id;
    private final String username;
    private final String secret;

    public User(Long id, String username, String secret) {
        this.id = id;
        this.username = username;
        this.secret = secret;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getSecret() { return secret; }
}
