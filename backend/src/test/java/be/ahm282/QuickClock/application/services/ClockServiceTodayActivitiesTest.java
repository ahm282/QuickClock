package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.QRTokenPort;
import be.ahm282.QuickClock.application.ports.out.QrScanNotificationPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClockServiceTodayActivitiesTest {

    @Mock
    private ClockRecordRepositoryPort clockRepo;

    @Mock
    private QRTokenPort qrTokenPort;

    @Mock
    private QrScanNotificationPort qrScanNotificationPort;

    private ClockService clockService;

    @BeforeEach
    void setUp() {
        clockService = new ClockService(clockRepo, qrTokenPort, qrScanNotificationPort);
    }

    @Test
    void testGetTodayActivities_shouldCallRepositoryWithCorrectTimeRange() {
        // Given
        Long userId = 1L;

        ClockRecord clockInRecord = new ClockRecord(1L, userId, ClockRecordType.IN, Instant.now(), null);
        ClockRecord clockOutRecord = new ClockRecord(2L, userId, ClockRecordType.OUT, Instant.now(), null);
        List<ClockRecord> expectedRecords = Arrays.asList(clockInRecord, clockOutRecord);

        when(clockRepo.findByUserIdAndRecordedAtBetween(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(expectedRecords);

        // When
        List<ClockRecord> result = clockService.getTodayActivities(userId);

        // Then
        assertEquals(2, result.size());
        assertEquals(expectedRecords, result);

        // Verify that the repository method was called with correct parameters
        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(clockRepo).findByUserIdAndRecordedAtBetween(eq(userId), startCaptor.capture(), endCaptor.capture());

        // Verify that start and end times represent today
        Instant startInstant = startCaptor.getValue();
        Instant endInstant = endCaptor.getValue();

        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        ZonedDateTime expectedStart = today.atStartOfDay(zoneId);
        ZonedDateTime expectedEnd = today.plusDays(1).atStartOfDay(zoneId);

        // Allow for small time differences in test execution
        assertTrue(Math.abs(startInstant.toEpochMilli() - expectedStart.toInstant().toEpochMilli()) < 1000);
        assertTrue(Math.abs(endInstant.toEpochMilli() - expectedEnd.toInstant().toEpochMilli()) < 1000);
    }

    @Test
    void testGetTodayActivities_shouldReturnEmptyListWhenNoActivities() {
        // Given
        Long userId = 1L;
        when(clockRepo.findByUserIdAndRecordedAtBetween(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        // When
        List<ClockRecord> result = clockService.getTodayActivities(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

