package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.TokenMetadata;
import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.AuthUseCase;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthenticationService implements AuthUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final TokenProviderPort tokenProviderPort;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    @Value("${app.security.dummy-hash}")
    private String dummyHash;

    public AuthenticationService(UserRepositoryPort userRepositoryPort,
                                 TokenProviderPort tokenProviderPort,
                                 PasswordEncoder passwordEncoder) throws Exception {
        this.userRepositoryPort = userRepositoryPort;
        this.tokenProviderPort = tokenProviderPort;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = SecureRandom.getInstanceStrong();
    }

    @Override
    public TokenPair login(String username, String password, TokenMetadata metadata) {
        Optional<User> maybeUser = userRepositoryPort.findByUsername(username);
        if (maybeUser.isEmpty()) {
            mitigateTimingAttack(password);
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = maybeUser.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            mitigateTimingAttack(password);
            throw new IllegalArgumentException("Invalid username or password");
        }

        String accessToken = tokenProviderPort.generateAccessToken(user.getUsername(), user.getId(), metadata);
        String refreshToken = tokenProviderPort.generateRefreshToken(user.getUsername(), user.getId(), metadata);

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

    private void mitigateTimingAttack(String password) {
        passwordEncoder.matches(password, dummyHash);
    }
}
