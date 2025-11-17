package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.domain.model.ClockRecord;

import java.util.List;

public interface ClockUseCase {
    ClockRecord clockIn(Long userId);
    ClockRecord clockOut(Long userId);
    ClockRecord clockInWithQR(String token);
    ClockRecord clockOutWithQR(String token);
    List<ClockRecord> getHistory(Long userId);
}
