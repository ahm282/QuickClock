package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.infrastructure.entity.ClockRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface JpaClockRecordRepository extends JpaRepository<ClockRecordEntity, Long> {
        List<ClockRecordEntity> findAllByUserIdOrderByRecordedAtDesc(Long userId);
        Optional<ClockRecordEntity> findTopByUserIdOrderByRecordedAtDesc(Long userId);
}
