package be.ahm282.QuickClock.application.ports.out;

import be.ahm282.QuickClock.domain.model.ClockRecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ClockRecordRepositoryPort {
    ClockRecord save(ClockRecord clockRecord);
    List<ClockRecord> findAllByUserId(Long userId);
    Optional<ClockRecord> findLatestByUserId(Long userId);
    List<ClockRecord> findByUserIdAndRecordedAtBetween(Long userId, Instant startOfDay, Instant endOfDay);
}
