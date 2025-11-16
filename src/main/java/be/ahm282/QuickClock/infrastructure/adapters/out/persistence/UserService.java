package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.domain.exception.NotFoundException;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class UserService {

    private final JpaUserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(JpaUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public UserEntity getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Get the secret for a user. Throws if user not found.
     */
    public String getSecretForUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getSecret)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Create a new user with a generated HMAC secret.
     */
    public UserEntity createUser(String username, String passwordHash) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("User already exists");
        }

        String secret = generateSecret();
        UserEntity user = new UserEntity(username, passwordHash, secret);
        return userRepository.save(user);
    }

    private String generateSecret() {
        byte[] bytes = new byte[32]; // 256-bit key
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
