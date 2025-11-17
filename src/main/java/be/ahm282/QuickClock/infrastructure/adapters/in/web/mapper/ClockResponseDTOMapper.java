package be.ahm282.QuickClock.infrastructure.adapters.in.web.mapper;

import be.ahm282.QuickClock.application.ports.in.dto.ClockResponseDTO;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import org.springframework.stereotype.Component;

@Component
public class ClockResponseDTOMapper {
    public ClockResponseDTO toDTO(ClockRecord record) {
        return new ClockResponseDTO(
                record.getId(),
                record.getUserId(),
                record.getType(),
                record.getTimestamp()
        );
    }
}