package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper;

import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.entity.ClockRecordEntity;
import org.springframework.stereotype.Component;

@Component
public class ClockRecordEntityMapper {
    public ClockRecordEntity toEntity(ClockRecord domain) {
        if (domain == null) return null;

        ClockRecordEntity entity = new ClockRecordEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setType(domain.getType());
        entity.setTimestamp(domain.getTimestamp());
        return entity;
    }

    public ClockRecord toDomain(ClockRecordEntity entity) {
        if (entity == null) return null;

        return new ClockRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getTimestamp()
        );
    }
}
