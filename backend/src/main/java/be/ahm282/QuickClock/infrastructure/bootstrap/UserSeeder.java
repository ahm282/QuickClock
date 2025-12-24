package be.ahm282.QuickClock.infrastructure.bootstrap;

import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.Role;
import be.ahm282.QuickClock.domain.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class UserSeeder {
    private final UserRepositoryPort userRepositoryPort;
    private final SecureRandom secureRandom;

    public UserSeeder(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.secureRandom = new SecureRandom();
    }

    @PostConstruct
    public void seed() {
        // Admin users
        seedUser("alice", "Alice", Set.of(Role.EMPLOYEE));
        seedUser("bob", "Bob", Set.of(Role.ADMIN));
        seedUser("ahmed.hassan", "Ahmed Hassan", Set.of(Role.ADMIN, Role.EMPLOYEE));
        seedUser("fatima.ali", "Fatima Ali", Set.of(Role.ADMIN));

        // Regular employees
        seedUser("mohamed.salah", "Mohamed Salah", Set.of(Role.EMPLOYEE));
        seedUser("nour.ibrahim", "Nour Ibrahim", Set.of(Role.EMPLOYEE));
        seedUser("omar.khaled", "Omar Khaled", Set.of(Role.EMPLOYEE));
        seedUser("layla.mahmoud", "Layla Mahmoud", Set.of(Role.EMPLOYEE));
        seedUser("youssef.ahmed", "Youssef Ahmed", Set.of(Role.EMPLOYEE));
        seedUser("sarah.mostafa", "Sarah Mostafa", Set.of(Role.EMPLOYEE));
        seedUser("karim.hassan", "Karim Hassan", Set.of(Role.EMPLOYEE));
        seedUser("mariam.said", "Mariam Said", Set.of(Role.EMPLOYEE));

        System.out.println("✅ Seeded 10 users successfully");
    }

    private void seedUser(String username, String displayName, Set<Role> roles) {
        if (userRepositoryPort.findByUsername(username).isPresent()) {
            return;
        }

        String secret = generateSecret();
        User user = User.newEmployee(
                username,
                displayName,
                "$2a$12$arCwvNjUX42eEOdol04eReAZnDKMx9MTUmF9xAgn4.OoJjvV3hrUi",
                secret,
                roles);
        userRepositoryPort.save(user);
    }

    private String generateSecret() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
