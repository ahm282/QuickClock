package be.ahm282.QuickClock.infrastructure.entity;

import be.ahm282.QuickClock.domain.model.AccountType;
import be.ahm282.QuickClock.domain.model.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_public_id", columnNames = "public_id"),
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_secret", columnNames = "secret")
},
        indexes = {
                @Index(name = "idx_users_public_id", columnList = "public_id"),
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_secret", columnList = "secret")
        })
public class UserEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID publicId = UUID.randomUUID();
    @Column(nullable = false, length = 32)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false, length = 100)
    private String secret;
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_roles_user")),
            indexes = {
                    @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
                    @Index(name = "idx_user_roles_role", columnList = "role"),
                    @Index(name = "idx_user_roles_user_id_role", columnList = "user_id, role")
            }
    )
    @Column(name = "role", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 32)
    private AccountType accountType = AccountType.EMPLOYEE;
    @Column(nullable = false, length = 64)
    private String displayName;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "last_login")
    private Instant lastLogin;
    @Column(name = "last_password_change", nullable = false)
    private Instant lastPasswordChange = Instant.now();
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;
    @Column(name = "locked_until")
    private Instant lockedUntil;

    public UserEntity() {
    }

    public UserEntity(Long id, UUID publicId, String username, String passwordHash, String secret, Set<Role> roles,
                      AccountType accountType, String displayName, boolean active, Instant lastLogin,
                      Instant lastPasswordChange, int failedLoginAttempts, Instant lockedUntil) {
        this.id = id;
        this.publicId = (publicId != null) ? publicId : UUID.randomUUID();
        this.username = username;
        this.passwordHash = passwordHash;
        this.secret = secret;
        this.displayName = displayName;
        this.active = active;
        this.lastLogin = lastLogin;
        this.lastPasswordChange = lastPasswordChange;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;

        setRoles(roles);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = (roles == null) ? new HashSet<>() : new HashSet<>(roles);
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Instant getLastPasswordChange() {
        return lastPasswordChange;
    }

    public void setLastPasswordChange(Instant lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    @Override
    protected void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }

        if (lastPasswordChange == null) {
            lastPasswordChange = Instant.EPOCH;
        }

        if (roles == null) {
            roles = new HashSet<>();
        }

        if (accountType == null) {
            accountType = AccountType.EMPLOYEE;
        }
    }
}
