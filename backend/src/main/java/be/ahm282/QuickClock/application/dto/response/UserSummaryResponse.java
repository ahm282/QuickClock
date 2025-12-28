package be.ahm282.QuickClock.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponse(
    UUID publicId,
    String displayName,
    String displayNameArabic,
    String lastClockType,  // "IN", "OUT", or null if never clocked
    Instant lastClockTime) {
}
