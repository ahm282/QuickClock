package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.application.ports.out.InvalidatedTokenRepositoryPort;
import be.ahm282.QuickClock.domain.model.InvalidatedToken;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper.InvalidatedTokenMapper;
import be.ahm282.QuickClock.infrastructure.entity.InvalidatedTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Component
public class JpaInvalidatedTokenRepositoryAdapter implements InvalidatedTokenRepositoryPort {
    private final JpaInvalidatedTokenRepository jpa;
    private final InvalidatedTokenMapper mapper;

    public JpaInvalidatedTokenRepositoryAdapter(JpaInvalidatedTokenRepository jpa, InvalidatedTokenMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<InvalidatedToken> findByJti(String jti) {
        return jpa.findByJti(jti).map(mapper::toDomain);
    }

    @Override
    public void save(InvalidatedToken invalidatedToken) {
        jpa.save(mapper.toEntity(invalidatedToken));
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserId(userId);
    }

    @Override
    public void deleteAllByExpiryTimeBefore(Instant cutoff) {
        jpa.deleteAllByExpiryTimeBefore(cutoff);
    }
}
