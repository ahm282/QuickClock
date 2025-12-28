package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.auth;

import be.ahm282.QuickClock.infrastructure.entity.InviteCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JpaInviteCodeRepository extends JpaRepository<InviteCodeEntity, Long> {
    Optional<InviteCodeEntity> findByCode(String code);
    List<InviteCodeEntity> findAllByUsedIsFalseAndRevokedIsFalseAndExpiresAtAfter(Instant now);
}
