package be.ahm282.QuickClock.infrastructure.adapters.in.web.mapper;

import be.ahm282.QuickClock.application.ports.in.dto.ClockResponseDTO;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockResponseMapperTest {

    private final ClockResponseMapper mapper = new ClockResponseMapper();

    @Test
    void shouldMapDomainToDTO() {
        ClockRecord record = new ClockRecord(1L, 42L, "IN", LocalDateTime.of(2025, 11, 12, 14, 0));
        ClockResponseDTO dto = mapper.toDTO(record);

        assertEquals(record.getId(), dto.getId());
        assertEquals(record.getUserId(), dto.getUserId());
        assertEquals(record.getType(), dto.getType());
        assertEquals(record.getTimestamp(), dto.getTimestamp());
    }
}
