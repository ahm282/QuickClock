package be.ahm282.QuickClock.infrastructure.bootstrap;

import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.Role;
import be.ahm282.QuickClock.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class InitialAdminAndKioskBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InitialAdminAndKioskBootstrap.class);

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.bootstrap.super-admin.username:}")
    private String superAdminUsername;

    @Value("${app.bootstrap.super-admin.password:}")
    private String superAdminPassword;

    @Value("${app.bootstrap.super-admin.displayName:}")
    private String superAdminDisplayName;

    @Value("${app.bootstrap.kiosk.username:}")
    private String kioskUsername;

    @Value("${app.bootstrap.kiosk.password:}")
    private String kioskPassword;

    public InitialAdminAndKioskBootstrap(UserRepositoryPort userRepositoryPort,
                                         PasswordEncoder passwordEncoder) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapSuperAdmin();
        bootstrapKioskUser();
    }

    private void bootstrapSuperAdmin() {
        if (isBlank(superAdminUsername)) {
            log.info("Super admin bootstrap username not configured; skipping super admin creation");
            return;
        }

        Optional<User> existingUser = userRepositoryPort.findByUsername(superAdminUsername);
        if (existingUser.isPresent()) {
            log.info("Super admin user '{}' already exists; skipping creation", superAdminUsername);
            return;
        }

        if (isBlank(superAdminPassword)) {
            log.warn("Super admin user '{}' does not exist but no bootstrap password is configured; NOT creating user", superAdminUsername);
            return;
        }

        String displayName = isBlank(superAdminDisplayName) ? "Super Admin" : superAdminDisplayName;
        String secret = generateSecret();
        User user = User.bootstrapServiceAccount(
                superAdminUsername,
                displayName,
                passwordEncoder.encode(superAdminPassword),
                secret,
                Set.of(Role.SUPER_ADMIN)
        );

        userRepositoryPort.save(user);

        log.warn("Super admin user '{}' created on startup. Rotate this password after first login.", superAdminUsername);
    }

    private void bootstrapKioskUser() {
        if (isBlank(kioskUsername)) {
            log.info("Kiosk bootstrap username not configured; skipping kiosk user creation");
            return;
        }

        Optional<User> existing = userRepositoryPort.findByUsername(kioskUsername);
        if (existing.isPresent()) {
            log.info("Kiosk user '{}' already exists; skipping creation", kioskUsername);
            return;
        }

        if (isBlank(kioskPassword)) {
            log.warn("Kiosk user '{}' does not exist but no bootstrap password is configured; NOT creating user", kioskUsername);
            return;
        }

        String secret = generateSecret();

        User user = User.bootstrapServiceAccount(
                kioskUsername,
                "Kiosk",
                passwordEncoder.encode(kioskPassword),
                secret,
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

