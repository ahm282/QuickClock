package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.AuthUseCase;
import be.ahm282.QuickClock.application.ports.out.*;
import be.ahm282.QuickClock.domain.exception.AuthenticationException;
import be.ahm282.QuickClock.domain.exception.ValidationException;
import be.ahm282.QuickClock.domain.model.InviteCode;
import be.ahm282.QuickClock.domain.model.RefreshToken;
import be.ahm282.QuickClock.domain.model.Role;
import be.ahm282.QuickClock.domain.model.User;
import io.jsonwebtoken.Claims;
import me.gosimple.nbvcxz.Nbvcxz;
import me.gosimple.nbvcxz.resources.Configuration;
import me.gosimple.nbvcxz.resources.ConfigurationBuilder;
import me.gosimple.nbvcxz.resources.Feedback;
import me.gosimple.nbvcxz.scoring.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class AuthenticationService implements AuthUseCase {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final InviteCodeRepositoryPort inviteCodeRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final TokenProviderPort tokenProviderPort;
    private final BreachedPasswordCheckPort breachedPasswordCheckPort;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final String dummyHash;
    private final Nbvcxz nbvcxz;

    public AuthenticationService(UserRepositoryPort userRepositoryPort,
                                 InviteCodeRepositoryPort inviteCodeRepositoryPort,
                                 RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                                 TokenProviderPort tokenProviderPort,
                                 PasswordEncoder passwordEncoder,
                                 BreachedPasswordCheckPort breachedPasswordCheckPort,
                                 @Value("${app.password.min-entropy:42}")
                                 double minimumPasswordEntropy) throws NoSuchAlgorithmException {
        this.userRepositoryPort = userRepositoryPort;
        this.inviteCodeRepositoryPort = inviteCodeRepositoryPort;
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.tokenProviderPort = tokenProviderPort;
        this.breachedPasswordCheckPort = breachedPasswordCheckPort;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = SecureRandom.getInstanceStrong();
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());

        Configuration passwordConfig = new ConfigurationBuilder()
                .setMinimumEntropy(minimumPasswordEntropy)
                .createConfiguration();

        this.nbvcxz = new Nbvcxz(passwordConfig);
    }

    @Override
    public TokenPair login(String username, String password) {
        Optional<User> maybeUser = userRepositoryPort.findByUsername(username);

        String hashToCheck = maybeUser.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(password, hashToCheck);

        if (maybeUser.isEmpty() || !passwordMatches) {
            throw new AuthenticationException("Authentication failed. Invalid username or password.");
        }

        User user = maybeUser.get();
        List<Role> roles = toRoleList(user);

        return issueInitialTokens(user, roles);
    }

    @Override
    public Long register(String username, String password, String inviteCode) {
        validatePassword(username, password);
        validateInviteCode(inviteCode);

        String passwordHash = passwordEncoder.encode(password);
        String secret = generateSecret();

        User toSave = new User(null, username, passwordHash, secret, Set.of(Role.EMPLOYEE));
        User savedUser = userRepositoryPort.save(toSave);

        markInviteCodeUsed(inviteCode, savedUser.getId());
        return savedUser.getId();
    }

    // ====================
    // Private helpers
    // ====================
    private List<Role> toRoleList(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(user.getRoles());
    }

    /**
     * Issues an access/refresh pair and stores the root refresh token in DB.
     * Used for initial login; refresh rotation is handled in RefreshTokenService.
     */
    private TokenPair issueInitialTokens(User user, List<Role> roles) {
        String username = user.getUsername();
        Long userId = user.getId();

        String accessToken = tokenProviderPort.generateAccessToken(username, userId, roles);
        String refreshToken = tokenProviderPort.generateRefreshToken(username, userId);

        UUID rootFamilyId = UUID.randomUUID();
        persistRefreshTokenAsRoot(refreshToken, rootFamilyId, userId);

        return new TokenPair(accessToken, refreshToken);
    }

    private void persistRefreshTokenAsRoot(String refreshToken, UUID rootFamilyId, Long userId) {
        Claims claims = tokenProviderPort.parseClaims(refreshToken);
        String jti = claims.getId();
        Instant expiry = claims.getExpiration().toInstant();
        Instant issuedAt = Instant.now();

        RefreshToken refreshTokenEntity = new RefreshToken(
                jti,
                null,                  // parentId = null → root
                rootFamilyId,
                userId,
                false,                 // revoked
                false,                 // used
                issuedAt,
                expiry
        );
        refreshTokenRepositoryPort.save(refreshTokenEntity);
    }

    private String generateSecret() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private void validatePassword(String username, String password) {
        if (password.equalsIgnoreCase(username)) {
            throw new ValidationException("Password must not be the same as username");
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        int buckets = 0;
        if (hasUpper) buckets++;
        if (hasLower) buckets++;
        if (hasDigit) buckets++;
        if (hasSymbol) buckets++;

        if (buckets < 2) {
            throw new ValidationException("Password is too weak");
        }

        Result result = nbvcxz.estimate(password);
        if (!result.isMinimumEntropyMet()) {
            Feedback feedback = result.getFeedback();
            String warning = feedback.getWarning();

            if (warning != null && !warning.isEmpty()) {
                throw new ValidationException("Password is too weak: " + warning);
            } else {
                throw new ValidationException("Password does not meet the minimum strength requirements.");
            }
        }

        // Check Have I Been Pwned
        try {
            int pwnedCount = breachedPasswordCheckPort.getBreachCount(password);
            if (pwnedCount > 0) {
                throw new ValidationException("This password has been exposed in a data breach " + pwnedCount + " times. Please choose a different password.");
            }
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            // Log the exception but do not prevent registration
            log.error("HIBP validation failed: {}", e.getMessage(), e);
        }
    }

    private void validateInviteCode(String code) {
        // TODO temporary: allow "ok" as a universal code during dev
        if (code.equals("ok")) {
            return;
        }

        var inviteOptional = inviteCodeRepositoryPort.findByCode(code);
        if (inviteOptional.isEmpty()) {
            throw new ValidationException("Invalid invite code");
        }

        var inviteCode = inviteOptional.get();
        if (inviteCode.isRevoked()) {
            throw new ValidationException("Invite code has been revoked and cannot be used");
        }

        if (inviteCode.isUsed() || inviteCode.isExpired()) {
            throw new ValidationException("Invite code has expired or already been used");
        }
    }

    private void markInviteCodeUsed(String code, Long userId) {
        if (code.equals("ok")) {
            return;
        }

        var invite = inviteCodeRepositoryPort.findByCode(code).orElseThrow(() ->
                new IllegalStateException("Invite code disappeared during registration"));
        InviteCode used = invite.markAsUsed(userId);
        inviteCodeRepositoryPort.save(used);
    }
}
