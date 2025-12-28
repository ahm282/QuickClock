package be.ahm282.QuickClock.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class User {

    private final Long id;
    private final UUID publicId;
    private final String username;
    private final String displayName;
    private final String displayNameArabic;
    private final String passwordHash;
    private final String secret;
    private final Set<Role> roles;
    private final AccountType accountType;
    private final boolean active;
    private final Instant lastLogin;
    private final Instant lastPasswordChange;
    private final int failedLoginAttempts;
    private final Instant lockedUntil;

    private User(Builder builder) {
        this.id = builder.id;
        this.publicId = builder.publicId;
        this.username = builder.username;
        this.displayName = builder.displayName;
        this.displayNameArabic = builder.displayNameArabic;
        this.passwordHash = builder.passwordHash;
        this.secret = builder.secret;

        this.roles = builder.roles == null ? Collections.emptySet() : Set.copyOf(builder.roles);
        this.accountType = builder.accountType == null ? AccountType.EMPLOYEE : builder.accountType;

        this.active = builder.active;
        this.lastLogin = builder.lastLogin;
        this.lastPasswordChange = builder.lastPasswordChange == null ? Instant.EPOCH : builder.lastPasswordChange;
        this.failedLoginAttempts = builder.failedLoginAttempts;
        this.lockedUntil = builder.lockedUntil;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ---------- Factories ----------

    public static User newEmployee(String username, String displayName, String displayNameArabic, String passwordHash, String secret, Set<Role> roles) {
        return User.builder()
                .username(username)
                .displayName(displayName)
                .displayNameArabic(displayNameArabic)
                .passwordHash(passwordHash)
                .secret(secret)
                .roles(roles)
                .accountType(AccountType.EMPLOYEE)
                .active(true)
                .lastPasswordChange(Instant.EPOCH)
                .build();
    }

    public static User bootstrapServiceAccount(String username, String displayName, String passwordHash, String secret, Set<Role> roles) {
        return User.builder()
                .username(username)
                .displayName(displayName)
                .passwordHash(passwordHash)
                .secret(secret)
                .roles(roles)
                .accountType(AccountType.SERVICE_ACCOUNT)
                .active(true)
                .lastPasswordChange(Instant.EPOCH)
                .build();
    }

    // ---------- The Builder Class ----------

    public static class Builder {
        private Long id;
        private UUID publicId;
        private String username;
        private String displayName;
        private String displayNameArabic;
        private String passwordHash;
        private String secret;
        private Set<Role> roles;
        private AccountType accountType;
        private boolean active;
        private Instant lastLogin;
        private Instant lastPasswordChange;
        private int failedLoginAttempts;
        private Instant lockedUntil;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder publicId(UUID publicId) { this.publicId = publicId; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder displayNameArabic(String displayNameArabic) { this.displayNameArabic = displayNameArabic; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder secret(String secret) { this.secret = secret; return this; }
        public Builder roles(Set<Role> roles) { this.roles = roles; return this; }
        public Builder accountType(AccountType accountType) { this.accountType = accountType; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder lastLogin(Instant lastLogin) { this.lastLogin = lastLogin; return this; }
        public Builder lastPasswordChange(Instant lastPasswordChange) { this.lastPasswordChange = lastPasswordChange; return this; }
        public Builder failedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; return this; }
        public Builder lockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; return this; }

        public User build() {
            return new User(this);
        }
    }

    // ---------- Getters (unchanged) ----------
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getDisplayNameArabic() { return displayNameArabic; }
    public String getPasswordHash() { return passwordHash; }
    public String getSecret() { return secret; }
    public Set<Role> getRoles() { return roles; }
    public AccountType getAccountType() { return accountType; }
    public boolean isActive() { return active; }
    public Instant getLastLogin() { return lastLogin; }
    public Instant getLastPasswordChange() { return lastPasswordChange; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
}