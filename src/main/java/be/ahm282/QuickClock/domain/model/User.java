package be.ahm282.QuickClock.domain.model;

import java.util.Collections;
import java.util.Set;

public class User {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String secret;
    private final Set<Role> roles;

    public User(Long id, String username, String passwordHash, String secret, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.secret = secret;
        this.roles = roles == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(Set.copyOf(roles));
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

    public Set<Role> getRoles() {
        return roles;
    }
}
