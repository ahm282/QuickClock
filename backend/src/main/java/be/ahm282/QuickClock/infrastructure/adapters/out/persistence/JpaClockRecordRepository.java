package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.infrastructure.entity.ClockRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public interface JpaClockRecordRepository extends JpaRepository<ClockRecordEntity, Long> {
        List<ClockRecordEntity> findAllByUserIdOrderByRecordedAtDesc(Long userId);
        Optional<ClockRecordEntity> findTopByUserIdOrderByRecordedAtDesc(Long userId);

        @Query("SELECT c FROM ClockRecordEntity c WHERE c.userId = :userId AND c.recordedAt >= :startOfDay AND c.recordedAt < :endOfDay ORDER BY c.recordedAt DESC")
        List<ClockRecordEntity> findByUserIdAndRecordedAtBetween(@Param("userId") Long userId, @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);
}
