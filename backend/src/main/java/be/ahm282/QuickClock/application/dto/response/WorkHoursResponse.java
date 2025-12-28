package be.ahm282.QuickClock.application.dto;

import org.decimal4j.immutable.Decimal1f;

public record WorkHoursDTO(
        Decimal1f hoursToday,
        Decimal1f hoursThisWeek
) {
}
