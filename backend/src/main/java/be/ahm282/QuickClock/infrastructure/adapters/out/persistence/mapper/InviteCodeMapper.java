package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper;

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

        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsed(domain.isUsed());
        entity.setRevoked(domain.isRevoked());
        entity.setUsedByUserId(domain.getUsedByUserId());
        entity.setCreatedByUserId(domain.getCreatedByUserId());
        entity.setRevokedByUserId(domain.getRevokedByUserId());
        entity.setRevokedAt(domain.getRevokedAt());

        return entity;
    }
}