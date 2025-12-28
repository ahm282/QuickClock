package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.auth;

import be.ahm282.QuickClock.infrastructure.entity.InvalidatedTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface JpaInvalidatedTokenRepository extends JpaRepository<InvalidatedTokenEntity, String> {
    Optional<InvalidatedTokenEntity> findByJti(String jti);

    @Modifying
    @Query("delete from InvalidatedTokenEntity e where e.userId = ?1")
    void deleteByUserId(Long userId);

    @Modifying
    @Query("delete from InvalidatedTokenEntity e where e.expiryTime <= ?1")
    void deleteAllByExpiryTimeBefore(Instant cutoff); // For cleanup job
}
