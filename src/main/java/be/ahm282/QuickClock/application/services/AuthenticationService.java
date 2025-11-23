package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.AuthUseCase;
import be.ahm282.QuickClock.application.ports.out.RefreshTokenRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.exception.AuthenticationException;
import be.ahm282.QuickClock.domain.exception.ValidationException;
import be.ahm282.QuickClock.domain.model.RefreshToken;
import be.ahm282.QuickClock.domain.model.User;
import io.jsonwebtoken.Claims;
import me.gosimple.nbvcxz.Nbvcxz;
import me.gosimple.nbvcxz.resources.Configuration;
import me.gosimple.nbvcxz.resources.ConfigurationBuilder;
import me.gosimple.nbvcxz.resources.Feedback;
import me.gosimple.nbvcxz.scoring.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService implements AuthUseCase {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String HIBP_API_URL = "https://api.pwnedpasswords.com/range/";
    private static final double MINIMUM_PASSWORD_ENTROPY = 42.0; // Secure enough without unnecessary friction

    private final UserRepositoryPort userRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final TokenProviderPort tokenProviderPort;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final String dummyHash;
    private final Nbvcxz nbvcxz;
    private final HttpClient httpClient;

    public AuthenticationService(UserRepositoryPort userRepositoryPort,
                                 RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                                 TokenProviderPort tokenProviderPort,
                                 PasswordEncoder passwordEncoder) throws Exception {
        this.userRepositoryPort = userRepositoryPort;
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.tokenProviderPort = tokenProviderPort;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = SecureRandom.getInstanceStrong();
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_2)
                .build();

        Configuration passwordConfig = new ConfigurationBuilder()
                .setMinimumEntropy(MINIMUM_PASSWORD_ENTROPY)
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

        // Generate tokens
        String accessToken = tokenProviderPort.generateAccessToken(user.getUsername(), user.getId());
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

        User toSave = new User(null, username, passwordHash, secret);
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
            int pwnedCount = checkHaveIBeenPwned(password);
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

    private int checkHaveIBeenPwned(String password) throws NoSuchAlgorithmException,
            IOException,
            InterruptedException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        String hash = bytesToHex(hashBytes).toUpperCase();

        String prefix = hash.substring(0, 5);
        String suffix = hash.substring(5);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(HIBP_API_URL + prefix))
                .header("Add-Padding", "true")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Have I Been Pwned API returned status {}: {}", response.statusCode(), response.body());
            throw new IOException("Error querying Have I Been Pwned API: " + response.statusCode());
        }

        for (String line : response.body().split("\r\n")) {
            String[] parts = line.split(":");
            if (parts[0].equalsIgnoreCase(suffix)) {
                return Integer.parseInt(parts[1]);
            }
        }
        return 0;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
