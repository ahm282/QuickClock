package be.ahm282.QuickClock.infrastructure.bootstrap;

import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.Role;
import be.ahm282.QuickClock.domain.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final BCryptPasswordEncoder passwordEncoder;

    public UserSeeder(UserRepositoryPort userRepositoryPort, BCryptPasswordEncoder passwordEncoder) {
        this.userRepositoryPort = userRepositoryPort;
        this.secureRandom = new SecureRandom();
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void seed() {
        // Admin users
        seedUser("alice", "Alice", "اليس", Set.of(Role.EMPLOYEE));
        seedUser("bob", "Bob", "بوب" ,Set.of(Role.ADMIN));
        seedUser("ahmed.hassan", "Ahmed Hassan", "احمد حسن", Set.of(Role.ADMIN, Role.EMPLOYEE));
        seedUser("fatima.ali", "Fatima Ali", "فاطمة علي", Set.of(Role.ADMIN)); // Added Arabic name

        // Regular employees
        seedUser("mohamed.salah", "Mohamed Salah", "محمد صلاح", Set.of(Role.EMPLOYEE)); // Added Arabic name
        seedUser("nour.ibrahim", "Nour Ibrahim", "نور ابراهيم", Set.of(Role.EMPLOYEE)); // Added Arabic name
        seedUser("omar.khaled", "Omar Khaled", "عمر خالد", Set.of(Role.EMPLOYEE));     // Added Arabic name
        seedUser("layla.mahmoud", "Layla Mahmoud", "ليلى محمود", Set.of(Role.EMPLOYEE)); // Added Arabic name
        seedUser("youssef.ahmed", "Youssef Ahmed", "يوسف احمد", Set.of(Role.EMPLOYEE)); // Added Arabic name
        seedUser("sarah.mostafa", "Sarah Mostafa", "سارة مصطفى", Set.of(Role.EMPLOYEE)); // Added Arabic name
        seedUser("karim.hassan", "Karim Hassan", "كريم حسن", Set.of(Role.EMPLOYEE));   // Added Arabic name
        seedUser("mariam.said", "Mariam Said", "مريم سعيد", Set.of(Role.EMPLOYEE));     // Added Arabic name

        System.out.println("✅ Seeded 10 users successfully");
    }

    private void seedUser(String username, String displayName, String displayNameArabic, Set<Role> roles) {
        if (userRepositoryPort.findByUsername(username).isPresent()) {
            return;
        }

        String secret = generateSecret();
        User user = User.newEmployee(
                username,
                displayName,
                displayNameArabic,
                passwordEncoder.encode("password123"),
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
