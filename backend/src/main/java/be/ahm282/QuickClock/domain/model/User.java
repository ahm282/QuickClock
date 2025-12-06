package be.ahm282.QuickClock.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class User {

    // Internal DB PK
    private final Long id;

    // Public/external identifier
    private final UUID publicId;

    private final String username;
    private final String displayName;

    private final String passwordHash;

    /**
     * Per-user secret used for QR HMAC signing.
     */
    private final String secret;

    private final Set<Role> roles;

    /**
     * Classification not equal to authorization.
     * EMPLOYEE shows up in kiosk lists.
     * SERVICE covers kiosk + super-admin bootstrap/service accounts.
     */
    private final AccountType accountType;

    private final boolean active;

    private final Instant lastLogin;

    private final Instant lastPasswordChange;

    private final int failedLoginAttempts;

    private final Instant lockedUntil;

    public User(
            Long id,
            UUID publicId,
            String username,
            String displayName,
            String passwordHash,
            String secret,
            Set<Role> roles,
            AccountType accountType,
            boolean active,
            Instant lastLogin,
            Instant lastPasswordChange,
            int failedLoginAttempts,
            Instant lockedUntil
    ) {
        this.id = id;
        this.publicId = publicId;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.secret = secret;

        this.roles = roles == null
                ? Collections.emptySet()
                : Set.copyOf(roles);

        this.accountType = accountType == null ? AccountType.EMPLOYEE : accountType;
        this.active = active;

        this.lastLogin = lastLogin;
        this.lastPasswordChange = lastPasswordChange == null ? Instant.EPOCH : lastPasswordChange;

        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
    }

    // ---------- Factories ----------

    public static User bootstrapServiceAccount(
            String username,
            String displayName,
            String passwordHash,
            String secret,
            Set<Role> roles
    ) {
        return new User(
                null,
                null,
                username,
                displayName,
                passwordHash,
                secret,
                roles,
                AccountType.SERVICE_ACCOUNT,
                true,
                null,
                Instant.EPOCH,
                0,
                null
        );
    }

    public static User newEmployee(
            String username,
            String displayName,
            String passwordHash,
            String secret,
            Set<Role> roles
    ) {
        return new User(
                null,
                null,
                username,
                displayName,
                passwordHash,
                secret,
                roles,
                AccountType.EMPLOYEE,
                true,
                null,
                Instant.EPOCH,
                0,
                null
        );
    }

    // ---------- Getters ----------

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
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

    public AccountType getAccountType() {
        return accountType;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public Instant getLastPasswordChange() {
        return lastPasswordChange;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
