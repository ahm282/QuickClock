package be.ahm282.QuickClock.application.ports.out;

import be.ahm282.QuickClock.domain.model.InvalidatedToken;

import java.time.Instant;
import java.util.Optional;

public interface InvalidatedTokenRepositoryPort {
    Optional<InvalidatedToken> findByJti(String jti);

    void save(InvalidatedToken entity);

    void deleteByUserId(Long userId);

    void deleteAllByExpiryTimeBefore(Instant cutoff);
}
