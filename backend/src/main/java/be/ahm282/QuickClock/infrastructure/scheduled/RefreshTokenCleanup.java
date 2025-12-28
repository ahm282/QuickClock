package be.ahm282.QuickClock.infrastructure.scheduled;

import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.JpaRefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled task to clean up expired refresh tokens.
 * Removes tokens that have expired to prevent database bloat.
 */
@Component
public class RefreshTokenCleanup {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);
    private final JpaRefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanup(JpaRefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Clean up expired refresh tokens every day at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")  // Every day at 3 AM
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running refresh token cleanup");
        Instant cutoff = Instant.now();
        refreshTokenRepository.deleteAllByExpiresAtBefore(cutoff);
        log.info("Refresh token cleanup completed");
    }
}

