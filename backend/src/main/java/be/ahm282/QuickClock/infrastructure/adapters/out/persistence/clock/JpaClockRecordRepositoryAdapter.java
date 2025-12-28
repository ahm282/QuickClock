package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.clock;

import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.mapper.ClockRecordEntityMapper;
import be.ahm282.QuickClock.infrastructure.entity.ClockRecordEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JpaClockRecordRepositoryAdapter implements ClockRecordRepositoryPort {
    private final JpaClockRecordRepository repository;
    private final ClockRecordEntityMapper mapper;

    public JpaClockRecordRepositoryAdapter(JpaClockRecordRepository repository, ClockRecordEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ClockRecord save(ClockRecord clockRecord) {
        ClockRecordEntity saved = repository.save(mapper.toEntity(clockRecord));
        return mapper.toDomain(saved);
    }

    @Override
    public List<ClockRecord> findAllByUserId(Long userId) {
        return repository.findAllByUserIdOrderByRecordedAtDesc(userId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ClockRecord> findLatestByUserId(Long userId) {
        return repository.findTopByUserIdOrderByRecordedAtDesc(userId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ClockRecord> findByUserIdAndRecordedAtBetween(Long userId, Instant startOfDay, Instant endOfDay) {
        return repository.findByUserIdAndRecordedAtBetween(userId, startOfDay, endOfDay)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
