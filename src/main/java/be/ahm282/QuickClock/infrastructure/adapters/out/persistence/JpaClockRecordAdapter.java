package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper.ClockRecordEntityMapper;
import be.ahm282.QuickClock.infrastructure.entity.ClockRecordEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JpaClockRecordAdapter implements ClockRecordRepositoryPort {
    private final JpaClockRecordRepositoryAdapter repository;
    private final ClockRecordEntityMapper mapper;

    public JpaClockRecordAdapter(JpaClockRecordRepositoryAdapter repository, ClockRecordEntityMapper mapper) {
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
                .sorted((cr1, cr2) -> cr2.getTimestamp().compareTo(cr1.getTimestamp()))
                .collect(Collectors.toList());
    }
}
