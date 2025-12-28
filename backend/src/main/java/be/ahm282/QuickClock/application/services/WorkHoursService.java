package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.response.WorkHoursResponse;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import be.ahm282.QuickClock.infrastructure.config.LocalizationConfig;
import org.decimal4j.immutable.Decimal1f;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class WorkHoursService {

    private final ClockRecordRepositoryPort clockRecordRepository;
    private final LocalizationConfig localizationConfig;
    private final Clock clock;

    private static final long MAX_SHIFT_HOURS = 12;

    public WorkHoursService(ClockRecordRepositoryPort clockRecordRepository,
                            LocalizationConfig localizationConfig,
                            Clock clock) {
        this.clockRecordRepository = clockRecordRepository;
        this.localizationConfig = localizationConfig;
        this.clock = clock;
    }

    /**
     * Calculate work hours for today and this week for a given user.
     * Uses configured timezone and week start day (e.g., Saturday for Egypt).
     */
    public WorkHoursResponse calculateWorkHours(Long userId) {
        // 1. Get ALL records and sort them by time
        List<ClockRecord> allRecords = new ArrayList<>(clockRecordRepository.findAllByUserId(userId));
        allRecords.sort(Comparator.comparing(ClockRecord::getRecordedAt));

        // 2. Setup Time Windows
        ZoneId zoneId = localizationConfig.getZoneId();
        Clock zoneClock = clock.withZone(zoneId);
        Instant now = clock.instant();

        // Today Window: [Today Midnight -> Now]
        LocalDate todayDate = LocalDate.now(zoneClock);
        Instant startOfToday = todayDate.atStartOfDay(zoneId).toInstant();

        // Week Window: [Start of Week Midnight -> Now]
        DayOfWeek weekStartDay = localizationConfig.getWeekStartDay();
        LocalDate startOfWeekDate = todayDate.with(TemporalAdjusters.previousOrSame(weekStartDay));
        Instant startOfWeek = startOfWeekDate.atStartOfDay(zoneId).toInstant();

        // 3. Calculate Overlaps
        // We pass ALL records. The method logic determines which parts of those records fall into the window.
        Decimal1f hoursToday = calculateHoursOverlap(allRecords, startOfToday, now);
        Decimal1f hoursThisWeek = calculateHoursOverlap(allRecords, startOfWeek, now);

        return new WorkHoursResponse(hoursToday, hoursThisWeek);
    }

    /**
     * Calculates the total duration of work sessions that intersect with the given [windowStart, windowEnd] interval.
     * Clamps shifts to a maximum of 12 hours.
     * This handles:
     * - Regular shifts
     * - Overnight shifts (splits hours correctly between days)
     * - Ongoing shifts (currently clocked in)
     */
    private Decimal1f calculateHoursOverlap(List<ClockRecord> records, Instant windowStart, Instant windowEnd) {
        long totalSeconds = 0;
        ClockRecord lastIn = null;

        for (ClockRecord record : records) {
            if (record.getType() == ClockRecordType.IN) {
                // If we hit an IN, this is the start of a potential new pair
                lastIn = record;
            } else if (record.getType() == ClockRecordType.OUT && lastIn != null) {
                // We have a closed pair: [IN -> OUT]
                Instant startWork = lastIn.getRecordedAt();
                Instant endWork = record.getRecordedAt();

                // --- Clamp shifts ---
                long rawDurationSeconds = Duration.between(startWork, endWork).getSeconds();
                long maxDurationSeconds = MAX_SHIFT_HOURS * 3600; // to hours

                if (rawDurationSeconds > maxDurationSeconds) {
                    // Scenario: User forgot to clock out yesterday.
                    // We cap the shift end time to (Start + 12 hours)
                    endWork = startWork.plusSeconds(maxDurationSeconds);
                }

                // Now calculate overlap using the (possibly clamped) end time
                totalSeconds += getOverlapDuration(startWork, endWork, windowStart, windowEnd);

                lastIn = null; // Reset the pair
            }
        }

        // Handle ongoing session (User is still clocked IN)
        if (lastIn != null) {
            Instant startWork = lastIn.getRecordedAt();

            // We also clamp ongoing shifts!
            // If they clocked in 24 hours ago and are still running, we shouldn't show 24 hours "Today".
            long currentDurationSeconds = Duration.between(startWork, windowEnd).getSeconds();
            long maxDurationSeconds = MAX_SHIFT_HOURS * 3600;

            Instant effectiveEnd = windowEnd;
            if (currentDurationSeconds > maxDurationSeconds) {
                effectiveEnd = startWork.plusSeconds(maxDurationSeconds);
            }

            totalSeconds += getOverlapDuration(startWork, effectiveEnd, windowStart, windowEnd);
        }

        return Decimal1f.valueOf(totalSeconds / 3600.0);
    }

    /**
     * Math helper: Returns the number of seconds two time intervals overlap.
     * Work Interval: [startWork, endWork]
     * Target Window: [windowStart, windowEnd]
     */
    private long getOverlapDuration(Instant startWork, Instant endWork, Instant windowStart, Instant windowEnd) {
        // 1. Find the later start time (The overlap starts at the latest of the two start times)
        Instant overlapStart = startWork.isAfter(windowStart) ? startWork : windowStart;

        // 2. Find the earlier end time (The overlap ends at the earliest of the two end times)
        Instant overlapEnd = endWork.isBefore(windowEnd) ? endWork : windowEnd;

        // 3. If start is before end, a valid overlap exists
        if (overlapStart.isBefore(overlapEnd)) {
            return Duration.between(overlapStart, overlapEnd).getSeconds();
        }
        return 0; // No overlap
    }
}