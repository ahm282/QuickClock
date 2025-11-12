package be.ahm282.QuickClock.infrastructure.entity;

import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder {

    private final UserService userService;

    public UserSeeder(UserService userService) {
        this.userService = userService;
    }

    @PostConstruct
    public void seed() {
        // Example: seed two users
        seedUser(1L, "alice", "$2a$10$cMvvFY1gy6ilpjxZg7CyIuchDr/Z7QKXPOsDzUhefU5nMeZ.cGAUu");
        seedUser(2L, "bob", "$2a$10$cMvvFY1gy6ilpjxZg7CyIuchDr/Z7QKXPOsDzUhefU5nMeZ.cGAUu");
    }

    private void seedUser(Long id, String username, String passwordHash) {
        try {
            userService.createUser(username, passwordHash);
        } catch (IllegalArgumentException e) {
            // User already exists, skip
        }
    }
}
