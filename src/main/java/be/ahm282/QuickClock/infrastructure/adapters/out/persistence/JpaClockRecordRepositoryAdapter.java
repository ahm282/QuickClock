package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.infrastructure.entity.ClockRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface JpaClockRecordRepositoryAdapter extends JpaRepository<ClockRecordEntity, Long> {
        List<ClockRecordEntity> findAllByUserIdOrderByTimestampDesc(Long userId);
}
