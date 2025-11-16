package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.infrastructure.entity.InvalidatedTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface JpaInvalidatedTokenRepository extends JpaRepository<InvalidatedTokenEntity, String> {
    Optional<InvalidatedTokenEntity> findByJti(String jti);
    void deleteByUserId(Long userId);
    void deleteByExpiryTimeBefore(Instant now); // For clean up job
}
