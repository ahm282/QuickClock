package be.ahm282.QuickClock.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AdminClockRequest(
        @NotNull
        Long userId,
        Instant recordedAtTimestamp,
        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {
}
