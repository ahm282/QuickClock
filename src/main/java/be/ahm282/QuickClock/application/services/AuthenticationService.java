package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.AuthUseCase;
import be.ahm282.QuickClock.application.ports.out.BreachedPasswordCheckPort;
import be.ahm282.QuickClock.application.ports.out.RefreshTokenRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.exception.AuthenticationException;
import be.ahm282.QuickClock.domain.exception.ValidationException;
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
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService implements AuthUseCase {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final double minimumPasswordEntropy; // Secure enough without unnecessary friction
    private final UserRepositoryPort userRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final TokenProviderPort tokenProviderPort;
    private final BreachedPasswordCheckPort breachedPasswordCheckPort;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final String dummyHash;
    private final Nbvcxz nbvcxz;

    public AuthenticationService(UserRepositoryPort userRepositoryPort,
                                 RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                                 TokenProviderPort tokenProviderPort,
                                 PasswordEncoder passwordEncoder,
                                 BreachedPasswordCheckPort breachedPasswordCheckPort,
                                 @Value("${app.password.min-entropy:42}")
                                 double minimumPasswordEntropy) throws NoSuchAlgorithmException {
        this.userRepositoryPort = userRepositoryPort;
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.tokenProviderPort = tokenProviderPort;
        this.breachedPasswordCheckPort = breachedPasswordCheckPort;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = SecureRandom.getInstanceStrong();
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
        this.minimumPasswordEntropy = minimumPasswordEntropy;

        Configuration passwordConfig = new ConfigurationBuilder()
                .setMinimumEntropy(this.minimumPasswordEntropy)
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

        // Valid user, proceed with token generation
        User user = maybeUser.get();
        Role role = user.getRole();
        List<Role> roles = (role != null) ? List.of(role) : List.of();

        // Generate tokens
        String accessToken = tokenProviderPort.generateAccessToken(user.getUsername(), user.getId(), roles);
        String refreshToken = tokenProviderPort.generateRefreshToken(user.getUsername(), user.getId());

        // Create new token family for this login session
        UUID rootFamilyId = UUID.randomUUID();

        // Parse refresh token to extract its JTI and expiry
        Claims claims = tokenProviderPort.parseClaims(refreshToken);
        String jti = claims.getId();
        Instant expiry = claims.getExpiration().toInstant();
        Instant issuedAt = Instant.now();

        // Store refresh token as the root of a new family
        RefreshToken refreshTokenEntity = new RefreshToken(
            jti,
            null,  // No parent - this is the root token
            rootFamilyId,
            user.getId(),
            false,
            false,
            issuedAt,
            expiry
        );
        refreshTokenRepositoryPort.save(refreshTokenEntity);

        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public Long register(String username, String password) {
        validatePassword(username, password);

        String passwordHash = passwordEncoder.encode(password);
        String secret = generateSecret();

        User toSave = new User(null, username, passwordHash, secret, Role.EMPLOYEE);
        User savedUser = userRepositoryPort.save(toSave);

        return savedUser.getId();
    }

    // ====================
    // Private helpers
    // ====================

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
}
