package be.ahm282.QuickClock.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AdminClockRequestDTO(
        @NotNull
        Long userId,
        Instant timestamp,
        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {
}
