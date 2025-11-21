package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.infrastructure.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    Optional<RefreshTokenEntity> findByJti(String jti);

    @Modifying
    @Query("UPDATE RefreshTokenEntity e SET e.revoked = true WHERE e.rootFamilyId = ?1")
    void revokeAllByFamilyId(UUID rootFamilyId);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity e WHERE e.userId = ?1")
    void deleteByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity e WHERE e.expiresAt <= ?1")
    void deleteAllByExpiresAtBefore(Instant cutoff);
}

