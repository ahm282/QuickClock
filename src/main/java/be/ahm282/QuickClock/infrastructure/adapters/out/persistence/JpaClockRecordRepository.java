package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.infrastructure.entity.ClockRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaClockRecordRepository extends JpaRepository<ClockRecordEntity, Long> {
        List<ClockRecordEntity> findAllByUserIdOrderByTimestampDesc(Long userId);
}
