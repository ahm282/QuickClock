package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.in.ClockUseCase;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.exception.ValidationException;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClockService implements ClockUseCase {
    private final ClockRecordRepositoryPort repository;

    public ClockService(ClockRecordRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public ClockRecord clockIn(Long userId) {
        List<ClockRecord> history = repository.findAllByUserId(userId);

        if (!history.isEmpty() && "IN".equals(history.getFirst().getType())) {
            throw new BusinessRuleException("User already clocked in. Must clock out before clocking in again.");
        }

        ClockRecord record = ClockRecord.create(userId, "IN");
        return repository.save(record);
    }

    @Override
    public ClockRecord clockOut(Long userId) {
        List<ClockRecord> history = repository.findAllByUserId(userId);

        if (history.isEmpty() || "OUT".equals(history.getFirst().getType())) {
            throw new BusinessRuleException("User must clock in before clocking out.");
        }

        ClockRecord record = ClockRecord.create(userId, "OUT");
        return repository.save(record);
    }

    @Override
    public List<ClockRecord> getHistory(Long userId) {
        if (userId == null) {
            throw new ValidationException("userId is required");
        }

        return repository.findAllByUserId(userId);
    }
}
