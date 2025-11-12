package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper.ClockRecordMapper;
import be.ahm282.QuickClock.infrastructure.entity.ClockRecordEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JpaClockRecordAdapter implements ClockRecordRepositoryPort {
    private final JpaClockRecordRepository repository;
    private final ClockRecordMapper mapper;

    public JpaClockRecordAdapter(JpaClockRecordRepository repository, ClockRecordMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ClockRecord save(ClockRecord clockRecord) {
        ClockRecordEntity saved = repository.save(mapper.toEntity(clockRecord));
        clockRecord.setId(saved.getId());
        return clockRecord;
    }

    @Override
    public List<ClockRecord> findAllByUserId(Long userId) {
        return repository.findAllByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
