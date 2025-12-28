package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.auth;

import be.ahm282.QuickClock.application.ports.out.RefreshTokenRepositoryPort;
import be.ahm282.QuickClock.domain.model.RefreshToken;
import be.ahm282.QuickClock.infrastructure.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter for refresh token repository port.
 */
@Repository
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {
    private final JpaRefreshTokenRepository jpa;
    private final RefreshTokenMapper mapper;

    public JpaRefreshTokenRepositoryAdapter(JpaRefreshTokenRepository jpa, RefreshTokenMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<RefreshToken> findByJti(String jti) {
        return jpa.findByJti(jti).map(mapper::toDomain);
    }

    @Override
    public void save(RefreshToken token) {
        jpa.save(mapper.toEntity(token));
    }

    @Override
    public void revokeAllByFamilyId(UUID rootFamilyId) {
        jpa.revokeAllByFamilyId(rootFamilyId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserId(userId);
    }

    @Override
    public void deleteAllByExpiresAtBefore(Instant cutoff) {
        jpa.deleteAllByExpiresAtBefore(cutoff);
    }
}

