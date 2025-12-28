package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.response.WorkHoursResponse;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import be.ahm282.QuickClock.infrastructure.config.LocalizationConfig;
import org.decimal4j.immutable.Decimal1f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkHoursServiceTest {

    @Mock
    private ClockRecordRepositoryPort clockRecordRepository;

    @Mock
    private LocalizationConfig localizationConfig;

    private WorkHoursService workHoursService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        // Configure for Egyptian timezone and week starting on Saturday
        when(localizationConfig.getZoneId()).thenReturn(ZoneId.of("Africa/Cairo"));
        when(localizationConfig.getWeekStartDay()).thenReturn(DayOfWeek.SATURDAY);

        // Fix: Set "Now" to Tuesday Night (23:00) so earlier records (like 17:00) are in the past.
        LocalDateTime tuesdayNight = LocalDateTime.of(2025, 12, 23, 23, 0);
        ZoneId cairo = ZoneId.of("Africa/Cairo");
        Instant fixedInstant = tuesdayNight.atZone(cairo).toInstant();

        fixedClock = Clock.fixed(fixedInstant, cairo);
        workHoursService = new WorkHoursService(clockRecordRepository, localizationConfig, fixedClock);
    }

    @Test
    void testCalculateHoursToday_singleSession() {
        // Given: User clocked in at 9 AM and out at 5 PM Cairo time today
        ZoneId cairo = ZoneId.of("Africa/Cairo");
        LocalDate today = LocalDate.now(fixedClock);

        Instant clockIn = today.atTime(9, 0).atZone(cairo).toInstant();
        Instant clockOut = today.atTime(17, 0).atZone(cairo).toInstant();

        List<ClockRecord> records = List.of(
                ClockRecord.createAt(1L, ClockRecordType.IN, clockIn, null),
                ClockRecord.createAt(1L, ClockRecordType.OUT, clockOut, null)
        );

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then: Should have 8 hours today
        assertEquals(8.0, asDouble(result.hoursToday()), 0.01);
    }

    @Test
    void testCalculateHoursToday_multipleSessions() {
        // Given: User worked two sessions today (Total 8.5 hours)
        ZoneId cairo = ZoneId.of("Africa/Cairo");
        LocalDate today = LocalDate.now(fixedClock);

        Instant session1In = today.atTime(8, 0).atZone(cairo).toInstant();
        Instant session1Out = today.atTime(12, 0).atZone(cairo).toInstant(); // 4 hours
        Instant session2In = today.atTime(13, 0).atZone(cairo).toInstant();
        Instant session2Out = today.atTime(17, 30).atZone(cairo).toInstant(); // 4.5 hours

        List<ClockRecord> records = List.of(
                ClockRecord.createAt(1L, ClockRecordType.IN, session1In, null),
                ClockRecord.createAt(1L, ClockRecordType.OUT, session1Out, null),
                ClockRecord.createAt(1L, ClockRecordType.IN, session2In, null),
                ClockRecord.createAt(1L, ClockRecordType.OUT, session2Out, null)
        );

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then: 4 + 4.5 = 8.5 hours
        assertEquals(8.5, asDouble(result.hoursToday()), 0.01);
    }

    @Test
    void testCalculateHoursToday_currentlyClockedIn() {
        // Given: User clocked in 2 hours ago (21:00) and is still clocked in (Now is 23:00)
        Instant twoHoursAgo = fixedClock.instant().minus(Duration.ofHours(2));

        List<ClockRecord> records = List.of(
                ClockRecord.createAt(1L, ClockRecordType.IN, twoHoursAgo, null)
        );

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then: 2 hours elapsed
        assertEquals(2.0, asDouble(result.hoursToday()), 0.01);
    }

    @Test
    void testClamping_forgottenClockOut() {
        // Given: User clocked in 20 hours ago (e.g., Today at 03:00 AM)
        // Since "Now" is 23:00, the duration is 20 hours.
        // Configured Max Shift is 12 hours.
        Instant twentyHoursAgo = fixedClock.instant().minus(Duration.ofHours(20));

        List<ClockRecord> records = List.of(
                ClockRecord.createAt(1L, ClockRecordType.IN, twentyHoursAgo, null)
        );

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then: Should be clamped to exactly 12.0 hours
        assertEquals(12.0, asDouble(result.hoursToday()), 0.01, "Should clamp 20h shift to 12h");
    }

    @Test
    void testCalculateHoursThisWeek_saturdayToFriday() {
        // Given: It's Tuesday in a week starting on Saturday
        ZoneId cairo = ZoneId.of("Africa/Cairo");
        LocalDate today = LocalDate.now(fixedClock);

        // Find this week's Saturday (start of week)
        LocalDate thisSaturday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));

        // Create records for Saturday, Sunday, Monday
        List<ClockRecord> records = new ArrayList<>();

        // Saturday: 6 hours
        records.add(ClockRecord.createAt(1L, ClockRecordType.IN,
                thisSaturday.atTime(9, 0).atZone(cairo).toInstant(), null));
        records.add(ClockRecord.createAt(1L, ClockRecordType.OUT,
                thisSaturday.atTime(15, 0).atZone(cairo).toInstant(), null));

        // Sunday: 7 hours
        LocalDate sunday = thisSaturday.plusDays(1);
        records.add(ClockRecord.createAt(1L, ClockRecordType.IN,
                sunday.atTime(9, 0).atZone(cairo).toInstant(), null));
        records.add(ClockRecord.createAt(1L, ClockRecordType.OUT,
                sunday.atTime(16, 0).atZone(cairo).toInstant(), null));

        // Monday: 8 hours
        LocalDate monday = thisSaturday.plusDays(2);
        records.add(ClockRecord.createAt(1L, ClockRecordType.IN,
                monday.atTime(9, 0).atZone(cairo).toInstant(), null));
        records.add(ClockRecord.createAt(1L, ClockRecordType.OUT,
                monday.atTime(17, 0).atZone(cairo).toInstant(), null));

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then: 6 + 7 + 8 = 21 hours
        assertEquals(21.0, asDouble(result.hoursThisWeek()), 0.01);
    }

    @Test
    void testUnmatchedClockIn_fromYesterday_clamped() {
        // Given: User clocked IN yesterday at 09:00 and never clocked out.
        // Current time: Today 23:00.
        // Raw duration: ~38 hours.
        // Clamping logic: Caps it at 12 hours. Effective End = Yesterday 21:00.

        ZoneId cairo = ZoneId.of("Africa/Cairo");
        LocalDate yesterday = LocalDate.now(fixedClock).minusDays(1);
        Instant yesterdayIn = yesterday.atTime(9, 0).atZone(cairo).toInstant();

        List<ClockRecord> records = List.of(
                ClockRecord.createAt(1L, ClockRecordType.IN, yesterdayIn, null)
        );

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then:
        // 1. Hours Today: 0.0 (Because the clamped shift ended yesterday at 21:00)
        assertEquals(0.0, asDouble(result.hoursToday()), 0.01);

        // 2. Hours Week: 12.0 (The clamped shift counts towards the weekly total)
        assertEquals(12.0, asDouble(result.hoursThisWeek()), 0.01);
    }

    @Test
    void testOvernightShift_spansTwoDays() {
        // Given: Shift from Yesterday 22:00 to Today 02:00 (4 Hours)
        // We use a specific clock setup for this test to control "Yesterday/Today" boundaries precisely
        LocalDateTime todayNight = LocalDateTime.of(2025, 12, 23, 23, 0);
        Instant fixedNow = todayNight.atZone(ZoneId.of("Africa/Cairo")).toInstant();
        Clock nightClock = Clock.fixed(fixedNow, ZoneId.of("Africa/Cairo"));

        workHoursService = new WorkHoursService(clockRecordRepository, localizationConfig, nightClock);

        LocalDate today = LocalDate.now(nightClock);
        ZoneId zone = ZoneId.of("Africa/Cairo");

        Instant clockIn = today.minusDays(1).atTime(22, 0).atZone(zone).toInstant();
        Instant clockOut = today.atTime(2, 0).atZone(zone).toInstant();

        List<ClockRecord> records = List.of(
                ClockRecord.createAt(1L, ClockRecordType.IN, clockIn, null),
                ClockRecord.createAt(1L, ClockRecordType.OUT, clockOut, null)
        );

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then:
        // Total shift is 4 hours.
        // 2 hours belong to Yesterday (22:00-00:00).
        // 2 hours belong to Today (00:00-02:00).
        assertEquals(2.0, asDouble(result.hoursToday()), 0.01, "Should count 2 hours from midnight to 2am");
    }

    // --- Helper Methods ---

    private static double asDouble(Decimal1f decimal) {
        return decimal == null ? 0.0 : decimal.doubleValue();
    }
}