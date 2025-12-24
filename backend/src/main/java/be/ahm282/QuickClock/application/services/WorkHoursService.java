package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.WorkHoursDTO;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import be.ahm282.QuickClock.infrastructure.config.LocalizationConfig;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

@Service
public class WorkHoursService {

    private final ClockRecordRepositoryPort clockRecordRepository;
    private final LocalizationConfig localizationConfig;

    public WorkHoursService(ClockRecordRepositoryPort clockRecordRepository,
                           LocalizationConfig localizationConfig) {
        this.clockRecordRepository = clockRecordRepository;
        this.localizationConfig = localizationConfig;
    }

    /**
     * Calculate work hours for today and this week for a given user.
     * Uses configured timezone and week start day (e.g., Saturday for Egypt).
     */
    public WorkHoursDTO calculateWorkHours(Long userId) {
        List<ClockRecord> allRecords = clockRecordRepository.findAllByUserId(userId);

        // Get current time and boundaries using the configured timezone
        ZoneId zoneId = localizationConfig.getZoneId();
        DayOfWeek weekStartDay = localizationConfig.getWeekStartDay();

        LocalDate today = LocalDate.now(zoneId);
        LocalDate startOfWeekDate = today.with(TemporalAdjusters.previousOrSame(weekStartDay));

        Instant startOfToday = today.atStartOfDay(zoneId).toInstant();
        Instant startOfWeek = startOfWeekDate.atStartOfDay(zoneId).toInstant();
        Instant now = Instant.now();

        // Filter records
        List<ClockRecord> todayRecords = allRecords.stream()
                .filter(r -> !r.getRecordedAt().isBefore(startOfToday))
                .sorted(Comparator.comparing(ClockRecord::getRecordedAt))
                .toList();

        List<ClockRecord> weekRecords = allRecords.stream()
                .filter(r -> !r.getRecordedAt().isBefore(startOfWeek))
                .sorted(Comparator.comparing(ClockRecord::getRecordedAt))
                .toList();

        double hoursToday = calculateHoursFromRecords(todayRecords, now);
        double hoursThisWeek = calculateHoursFromRecords(weekRecords, now);

        return new WorkHoursDTO(hoursToday, hoursThisWeek);
    }

    /**
     * Calculate total hours from a list of clock records.
     * Pairs IN/OUT records and sums the duration.
     * If the last record is an IN without an OUT, calculates up to 'now'.
     */
    private double calculateHoursFromRecords(List<ClockRecord> records, Instant now) {
        long totalSeconds = 0;
        ClockRecord lastIn = null;

        for (ClockRecord record : records) {
            if (record.getType() == ClockRecordType.IN) {
                lastIn = record;
            } else if (record.getType() == ClockRecordType.OUT && lastIn != null) {
                // Calculate duration between IN and OUT
                long duration = Duration.between(lastIn.getRecordedAt(), record.getRecordedAt()).getSeconds();
                totalSeconds += duration;
                lastIn = null; // Reset after pairing
            }
        }

        // If user is currently clocked in (last record is IN without OUT)
        if (lastIn != null) {
            long duration = Duration.between(lastIn.getRecordedAt(), now).getSeconds();
            totalSeconds += duration;
        }

        // Convert seconds to hours (with decimal precision)
        return totalSeconds / 3600.0;
    }
}

