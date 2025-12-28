package be.ahm282.QuickClock.infrastructure.mapper;

import be.ahm282.QuickClock.domain.model.InviteCode;
import be.ahm282.QuickClock.infrastructure.entity.InviteCodeEntity;
import org.springframework.stereotype.Component;

@Component
public class InviteCodeMapper {
    public InviteCode toDomain(InviteCodeEntity entity) {
        if (entity == null) {
            return null;
        }

        return new InviteCode(
                entity.getId(),
                entity.getCode(),
                entity.getExpiresAt(),
                entity.isUsed(),
                entity.isRevoked(),
                entity.getRevokedByUserId(),
                entity.getRevokedAt(),
                entity.getUsedByUserId(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt()
        );
    }

    public InviteCodeEntity toEntity(InviteCode domain) {
        if (domain == null) {
            return null;
        }

        InviteCodeEntity entity = new InviteCodeEntity();

        entity.setId(domain.id());
        entity.setCode(domain.code());
        entity.setExpiresAt(domain.expiresAt());
        entity.setUsed(domain.used());
        entity.setRevoked(domain.revoked());
        entity.setUsedByUserId(domain.usedByUserId());
        entity.setCreatedByUserId(domain.createdByUserId());
        entity.setRevokedByUserId(domain.revokedByUserId());
        entity.setRevokedAt(domain.revokedAt());

        return entity;
    }
}