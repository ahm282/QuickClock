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
                entity.getUsedByUserId()
        );
    }

    public InviteCodeEntity toEntity(InviteCode domain) {
        InviteCodeEntity entity = new InviteCodeEntity();
        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsed(domain.isUsed());
        entity.setUsedByUserId(domain.getUsedByUserId());
        return entity;
    }
}
