package be.ahm282.QuickClock.infrastructure.scheduled;

import be.ahm282.QuickClock.infrastructure.security.service.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to clean up stale rate limit buckets.
 */
@Component
public class RateLimitCleanup {
    private static final Logger log = LoggerFactory.getLogger(RateLimitCleanup.class);
    private final RateLimitService rateLimitService;

    public RateLimitCleanup(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * Clean up rate limit buckets every hour.
     * Removes buckets that are full (indicating no recent activity).
     */
    @Scheduled(cron = "0 0 * * * *")  // Every hour at minute 0
    public void cleanupRateLimitBuckets() {
        log.debug("Running rate limit bucket cleanup");
        rateLimitService.cleanup();
    }
}

