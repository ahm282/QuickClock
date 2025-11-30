package be.ahm282.QuickClock.application.ports.in.dto;

import be.ahm282.QuickClock.domain.model.ClockRecordType;

import java.time.Instant;

public record ClockResponseDTO(
        Long id,
        Long userId,
        ClockRecordType type,
        Instant timestamp,
        String reason // new
) {
}
