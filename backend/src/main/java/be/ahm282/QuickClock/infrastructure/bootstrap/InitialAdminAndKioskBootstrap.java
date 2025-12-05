package be.ahm282.QuickClock.infrastructure.bootstrap;

import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.Role;
import be.ahm282.QuickClock.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

@Component
public class InitialAdminAndKioskBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InitialAdminAndKioskBootstrap.class);

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    @Value("${app.bootstrap.super-admin.username:}")
    private String superAdminUsername;

    @Value("${app.bootstrap.super-admin.password:}")
    private String superAdminPassword;

    @Value("${app.bootstrap.kiosk.username:}")
    private String kioskUsername;

    @Value("${app.bootstrap.kiosk.password:}")
    private String kioskPassword;

    public InitialAdminAndKioskBootstrap(UserRepositoryPort userRepositoryPort,
                                         PasswordEncoder passwordEncoder) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstraSuperAdmin();
        bootstrapKioskUser();
    }

    private void bootstraSuperAdmin() {
        if (superAdminUsername == null || superAdminPassword.isBlank()) {
            log.info("Super admin bootstrap username not configured; skipping super admin creation");
            return;
        }

        Optional<User> existingUser = userRepositoryPort.findByUsername(superAdminUsername);
        if (existingUser.isPresent()) {
            log.info("Super admin user '{}' already exists; skipping creation", superAdminUsername);
            return;
        }

        if (superAdminPassword == null || superAdminPassword.isBlank()) {
            log.warn("Super admin user '{}' does not exist but no bootstrap password is configured; NOT creating user", superAdminUsername);
            return;
        }

        User user = new User(
                null,
                superAdminUsername,
                passwordEncoder.encode(superAdminPassword),
                generateSecret(),
                Set.of(Role.SUPER_ADMIN)
        );
        userRepositoryPort.save(user);
        // TODO: add last login audit entry
        log.warn("Super admin user '{}' created on startup. MAKE SURE to rotate this password after first login.", superAdminUsername);
    }

    private void bootstrapKioskUser() {
        if (kioskUsername == null || kioskUsername.isBlank()) {
            log.info("Kiosk bootstrap username not configured; skipping kiosk user creation");
            return;
        }

        Optional<User> existing = userRepositoryPort.findByUsername(kioskUsername);
        if (existing.isPresent()) {
            log.info("Kiosk user '{}' already exists; skipping creation", kioskUsername);
            return;
        }

        if (kioskPassword == null || kioskPassword.isBlank()) {
            log.warn("Kiosk user '{}' does not exist but no bootstrap password is configured; NOT creating user", kioskUsername);
            return;
        }

        User user = new User(
                null,
                kioskUsername,
                passwordEncoder.encode(kioskPassword),
                generateSecret(),
                Set.of(Role.KIOSK)
        );
        userRepositoryPort.save(user);
        log.warn("Kiosk user '{}' created on startup. Lock credentials down on the kiosk device.", kioskUsername);
    }

    private String generateSecret() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
