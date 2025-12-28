package be.ahm282.QuickClock.infrastructure.mapper;

import be.ahm282.QuickClock.application.dto.response.ClockResponse;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import org.springframework.stereotype.Component;

@Component
public class ClockResponseMapper {
    public ClockResponse toDTO(ClockRecord record) {
        return new ClockResponse(
                record.getId(),
                record.getUserId(),
                record.getType(),
                record.getRecordedAt(),
                record.getReason()
        );
    }
}