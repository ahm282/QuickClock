package be.ahm282.QuickClock.application.ports.out;

import be.ahm282.QuickClock.domain.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for refresh token repository operations.
 */
public interface RefreshTokenRepositoryPort {
    Optional<RefreshToken> findByJti(String jti);

    void save(RefreshToken token);

    void revokeAllByFamilyId(UUID rootFamilyId);

    void deleteByUserId(Long userId);

    void deleteAllByExpiresAtBefore(Instant cutoff);
}

