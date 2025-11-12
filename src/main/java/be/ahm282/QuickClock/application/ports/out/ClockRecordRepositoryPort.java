package be.ahm282.QuickClock.application.ports.out;

import be.ahm282.QuickClock.domain.model.ClockRecord;

import java.util.List;

public interface ClockRecordRepositoryPort {
    ClockRecord save(ClockRecord clockRecord);
    List<ClockRecord> findAllByUserId(Long userId);
}
