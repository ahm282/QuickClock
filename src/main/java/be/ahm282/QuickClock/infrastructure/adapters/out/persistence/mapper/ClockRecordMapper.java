package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper;

import be.ahm282.QuickClock.application.ports.in.dto.ClockResponseDTO;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.entity.ClockRecordEntity;
import org.springframework.stereotype.Component;

@Component
public class ClockRecordMapper {
    public ClockRecordEntity toEntity(ClockRecord domain) {
        ClockRecordEntity entity = new ClockRecordEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setType(domain.getType());
        entity.setTimestamp(domain.getTimestamp());
        return entity;
    }

    public ClockRecord toDomain(ClockRecordEntity entity) {
        ClockRecord record = new ClockRecord();
        record.setId(entity.getId());
        record.setUserId(entity.getUserId());
        record.setType(entity.getType());
        record.setTimestamp(entity.getTimestamp());
        return record;
    }

    @Component
    public static class ClockResponseMapper {
        public ClockResponseDTO toDTO(ClockRecord record) {
            return new ClockResponseDTO(
                    record.getId(),
                    record.getUserId(),
                    record.getType(),
                    record.getTimestamp()
            );
        }
    }
}
