package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.domain.model.ClockRecord;

import java.time.Instant;
import java.util.List;

public interface ClockUseCase {
    ClockRecord clockIn(Long userId);
    ClockRecord clockOut(Long userId);
    ClockRecord clockInWithQR(String token, Long authenticatedUserId);
    ClockRecord clockOutWithQR(String token, Long authenticatedUserId);
    List<ClockRecord> getHistory(Long userId);

    // Admin corrections
    ClockRecord adminClockIn(Long userId, Instant timestamp, String reason);
    ClockRecord adminClockOut(Long userId, Instant timestamp, String reason);
}
