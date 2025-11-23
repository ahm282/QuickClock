package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.AuthUseCase;
import be.ahm282.QuickClock.application.ports.out.RefreshTokenRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.RefreshToken;
import be.ahm282.QuickClock.domain.model.User;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService implements AuthUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final TokenProviderPort tokenProviderPort;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final String dummyHash;

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
    }

    @Override
    public TokenPair login(String username, String password) {
        Optional<User> maybeUser = userRepositoryPort.findByUsername(username);

        String hashToCheck = maybeUser.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(password, hashToCheck);

        if (maybeUser.isEmpty() || !passwordMatches) {
            throw new IllegalArgumentException("Invalid username or password");
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
            user.getUsername(),
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
        String passwordHash = passwordEncoder.encode(password);
        String secret = generateSecret();

        User toSave = new User(null, username, passwordHash, secret);
        User savedUser = userRepositoryPort.save(toSave);

        return savedUser.getId();
    }

    private String generateSecret() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
