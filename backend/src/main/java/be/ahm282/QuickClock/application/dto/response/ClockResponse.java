package be.ahm282.QuickClock.application.dto.response;

import be.ahm282.QuickClock.domain.model.ClockRecordType;

import java.time.Instant;

public record ClockResponse(
        Long id,
        Long userId,
        ClockRecordType type,
        Instant recordedAt,
        String reason // new
) {
}
