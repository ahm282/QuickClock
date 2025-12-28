package be.ahm282.QuickClock.infrastructure.scheduled;

import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.auth.JpaInvalidatedTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InvalidatedTokenCleanup {
    private final JpaInvalidatedTokenRepository invalidatedTokenRepository;

    public InvalidatedTokenCleanup(JpaInvalidatedTokenRepository invalidatedTokenRepository) {
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        invalidatedTokenRepository.deleteAllByExpiryTimeBefore(Instant.now());
    }
}
