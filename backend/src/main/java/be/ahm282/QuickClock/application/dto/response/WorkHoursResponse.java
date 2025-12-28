package be.ahm282.QuickClock.application.dto.response;

import org.decimal4j.immutable.Decimal1f;

public record WorkHoursResponse(
        Decimal1f hoursToday,
        Decimal1f hoursThisWeek
) {
}
