package be.ahm282.QuickClock.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryView(
    UUID publicId,
    String displayName,
    String lastClockType,  // "IN", "OUT", or null if never clocked
    Instant lastClockTime) {
}
