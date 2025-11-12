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
        seedUser(1L, "alice");
        seedUser(2L, "bob");
    }

    private void seedUser(Long id, String username) {
        try {
            userService.createUser(id, username);
        } catch (IllegalArgumentException e) {
            // User already exists, skip
        }
    }
}
