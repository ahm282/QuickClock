package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.WorkHoursResponse;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import be.ahm282.QuickClock.infrastructure.config.LocalizationConfig;
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

    @BeforeEach
    void setUp() {
        // Configure for Egyptian timezone and week starting on Saturday
        when(localizationConfig.getZoneId()).thenReturn(ZoneId.of("Africa/Cairo"));
        when(localizationConfig.getWeekStartDay()).thenReturn(DayOfWeek.SATURDAY);

        workHoursService = new WorkHoursService(clockRecordRepository, localizationConfig);
    }

    @Test
    void testCalculateHoursToday_singleSession() {
        // Given: User clocked in at 9 AM and out at 5 PM Cairo time today
        ZoneId cairo = ZoneId.of("Africa/Cairo");
        LocalDate today = LocalDate.now(cairo);

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
        assertEquals(8.0, result.hoursToday(), 0.01);
    }

    @Test
    void testCalculateHoursToday_multipleSessions() {
        // Given: User worked two sessions today
        ZoneId cairo = ZoneId.of("Africa/Cairo");
        LocalDate today = LocalDate.now(cairo);

        Instant session1In = today.atTime(8, 0).atZone(cairo).toInstant();
        Instant session1Out = today.atTime(12, 0).atZone(cairo).toInstant();
        Instant session2In = today.atTime(13, 0).atZone(cairo).toInstant();
        Instant session2Out = today.atTime(17, 30).atZone(cairo).toInstant();

        List<ClockRecord> records = List.of(
            ClockRecord.createAt(1L, ClockRecordType.IN, session1In, null),
            ClockRecord.createAt(1L, ClockRecordType.OUT, session1Out, null),
            ClockRecord.createAt(1L, ClockRecordType.IN, session2In, null),
            ClockRecord.createAt(1L, ClockRecordType.OUT, session2Out, null)
        );

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then: 4 hours (morning) + 4.5 hours (afternoon) = 8.5 hours
        assertEquals(8.5, result.hoursToday(), 0.01);
    }

    @Test
    void testCalculateHoursToday_currentlyClockedIn() {
        // Given: User clocked in 2 hours ago and is still clocked in
        // Use a recent timestamp to simulate "currently clocked in"
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));

        List<ClockRecord> records = List.of(
            ClockRecord.createAt(1L, ClockRecordType.IN, twoHoursAgo, null)
        );

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then: Should have hours today if the clock-in was within today's boundaries in Cairo time
        // This test might have 0 hours if run at midnight, or full hours if clock-in was today
        assertTrue(result.hoursToday() >= 0.0,
            "Hours today should be non-negative, got: " + result.hoursToday());

        // Week hours should definitely include this session
        assertTrue(result.hoursThisWeek() >= 1.9,
            "Week hours should include the 2-hour session, got: " + result.hoursThisWeek());
    }

    @Test
    void testCalculateHoursThisWeek_saturdayToFriday() {
        // Given: It's Tuesday in a week starting on Saturday
        ZoneId cairo = ZoneId.of("Africa/Cairo");
        LocalDate today = LocalDate.now(cairo);

        // Find this week's Saturday (start of week)
        LocalDate thisSaturday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));

        // Create records for Saturday, Sunday, Monday, Tuesday
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

        // Then: Week hours should include all days from Saturday onwards
        assertTrue(result.hoursThisWeek() >= 21.0); // At least 6+7+8 = 21 hours
    }

    @Test
    void testEmptyRecords() {
        // Given: User has no clock records
        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(List.of());

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then: Should have 0 hours
        assertEquals(0.0, result.hoursToday(), 0.01);
        assertEquals(0.0, result.hoursThisWeek(), 0.01);
    }

    @Test
    void testUnmatchedClockIn_ignored() {
        // Given: User has a clock IN without corresponding OUT from yesterday
        ZoneId cairo = ZoneId.of("Africa/Cairo");
        LocalDate yesterday = LocalDate.now(cairo).minusDays(1);

        Instant yesterdayIn = yesterday.atTime(9, 0).atZone(cairo).toInstant();

        List<ClockRecord> records = List.of(
            ClockRecord.createAt(1L, ClockRecordType.IN, yesterdayIn, null)
            // No OUT record - unmatched
        );

        when(clockRecordRepository.findAllByUserId(1L)).thenReturn(records);

        // When
        WorkHoursResponse result = workHoursService.calculateWorkHours(1L);

        // Then: Should not count in today's hours but should count in week hours (as ongoing)
        assertEquals(0.0, result.hoursToday(), 0.01);
        // Week hours will be substantial since it's calculating from yesterday until now
        assertTrue(result.hoursThisWeek() > 0.0);
    }
}
