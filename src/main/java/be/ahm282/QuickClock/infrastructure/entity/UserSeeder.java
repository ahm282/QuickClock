package be.ahm282.QuickClock.infrastructure.entity;

import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.Role;
import be.ahm282.QuickClock.domain.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class UserSeeder {
    private final UserRepositoryPort userRepositoryPort;
    private final SecureRandom secureRandom;

    public UserSeeder(UserRepositoryPort userRepositoryPort) throws Exception {
        this.userRepositoryPort = userRepositoryPort;
        this.secureRandom = SecureRandom.getInstanceStrong();
    }

    @PostConstruct
    public void seed() {
        seedUser("alice");
        seedUser("bob");
    }

    private void seedUser(String username) {
        if (userRepositoryPort.findByUsername(username).isPresent()) {
            return;
        }

        String secret = generateSecret();
        User user = new User(null, username, "$2a$10$cMvvFY1gy6ilpjxZg7CyIuchDr/Z7QKXPOsDzUhefU5nMeZ.cGAUu", secret, Role.EMPLOYEE);
        userRepositoryPort.save(user);
    }

    private String generateSecret() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
