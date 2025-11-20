package be.ahm282.QuickClock.infrastructure.security.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service using Bucket4j (Token Bucket algorithm).
 */
@Service
public class RateLimitService {
    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    // Different rate limit configurations for different endpoints
    private static final int LOGIN_CAPACITY = 5;  // 5 attempts
    private static final Duration LOGIN_REFILL_PERIOD = Duration.ofMinutes(5);  // per 5 minutes

    private static final int REFRESH_CAPACITY = 10;  // 10 attempts
    private static final Duration REFRESH_REFILL_PERIOD = Duration.ofMinutes(1);  // per minute

    private static final int REGISTER_CAPACITY = 3;  // 3 attempts
    private static final Duration REGISTER_REFILL_PERIOD = Duration.ofMinutes(10);  // per 10 minutes

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    /**
     * Check if a login attempt is allowed for the given identifier.
     *
     * @param identifier Unique identifier (e.g., IP:username)
     * @return true if allowed, false if rate-limited
     */
    public boolean allowLoginAttempt(String identifier) {
        Bucket bucket = loginBuckets.computeIfAbsent(identifier, k -> createLoginBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Login rate limit exceeded for: {}", identifier);
        }

        return allowed;
    }

    /**
     * Check if a refresh attempt is allowed for the given identifier.
     *
     * @param identifier Unique identifier (e.g., IP or deviceId)
     * @return true if allowed, false if rate-limited
     */
    public boolean allowRefreshAttempt(String identifier) {
        Bucket bucket = refreshBuckets.computeIfAbsent(identifier, k -> createRefreshBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Refresh rate limit exceeded for: {}", identifier);
        }

        return allowed;
    }

    /**
     * Check if a registration attempt is allowed for the given identifier.
     *
     * @param identifier Unique identifier (typically IP address)
     * @return true if allowed, false if rate-limited
     */
    public boolean allowRegisterAttempt(String identifier) {
        Bucket bucket = registerBuckets.computeIfAbsent(identifier, k -> createRegisterBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Registration rate limit exceeded for: {}", identifier);
        }

        return allowed;
    }

    /**
     * Reset rate limit for successful login (optional - allows immediate retry after success).
     *
     * @param identifier Unique identifier
     */
    public void resetLoginLimit(String identifier) {
        loginBuckets.remove(identifier);
    }

    /**
     * Reset rate limit for successful refresh.
     *
     * @param identifier Unique identifier
     */
    public void resetRefreshLimit(String identifier) {
        refreshBuckets.remove(identifier);
    }

    /**
     * Get remaining attempts for login (for debugging/monitoring).
     *
     * @param identifier Unique identifier
     * @return Number of remaining attempts
     */
    public long getRemainingLoginAttempts(String identifier) {
        Bucket bucket = loginBuckets.get(identifier);
        return bucket != null ? bucket.getAvailableTokens() : LOGIN_CAPACITY;
    }

    // Factory methods for creating buckets

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(LOGIN_CAPACITY)
                .refillGreedy(LOGIN_CAPACITY, LOGIN_REFILL_PERIOD)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createRefreshBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(REFRESH_CAPACITY)
                .refillGreedy(REFRESH_CAPACITY, REFRESH_REFILL_PERIOD)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createRegisterBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(REGISTER_CAPACITY)
                .refillGreedy(REGISTER_CAPACITY, REGISTER_REFILL_PERIOD)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Cleanup method - can be scheduled to run periodically.
     * Removes buckets that are full (indicating no recent activity).
     */
    public void cleanup() {
        loginBuckets.entrySet().removeIf(entry ->
            entry.getValue().getAvailableTokens() >= LOGIN_CAPACITY);
        refreshBuckets.entrySet().removeIf(entry ->
            entry.getValue().getAvailableTokens() >= REFRESH_CAPACITY);
        registerBuckets.entrySet().removeIf(entry ->
            entry.getValue().getAvailableTokens() >= REGISTER_CAPACITY);
    }
}

