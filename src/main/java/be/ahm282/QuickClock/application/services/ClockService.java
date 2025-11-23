package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.in.ClockUseCase;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.QRTokenPort;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ClockService implements ClockUseCase {

    private final ClockRecordRepositoryPort clockRepo;
    private final QRTokenPort qrTokenPort;

    public ClockService(ClockRecordRepositoryPort clockRepo, QRTokenPort qrTokenPort) {
        this.clockRepo = clockRepo;
        this.qrTokenPort = qrTokenPort;
    }

    @Override
    public ClockRecord clockIn(Long userId) {
        checkClockInRules(userId);
        ClockRecord record = ClockRecord.create(userId, ClockRecordType.IN);
        return clockRepo.save(record);
    }

    @Override
    public ClockRecord clockOut(Long userId) {
        checkClockOutRules(userId);
        ClockRecord record = ClockRecord.create(userId, ClockRecordType.OUT);
        return clockRepo.save(record);
    }

    @Override
    public ClockRecord clockInWithQR(String qrToken) {
        Long userId = qrTokenPort.validateAndExtractUserId(qrToken);
        return clockIn(userId);
    }

    @Override
    public ClockRecord clockOutWithQR(String qrToken) {
        Long userId = qrTokenPort.validateAndExtractUserId(qrToken);
        return clockOut(userId);
    }

    @Override
    public List<ClockRecord> getHistory(Long userId) {
        return clockRepo.findAllByUserId(userId);
    }

    private void checkClockInRules(Long userId) {
        clockRepo.findLatestByUserId(userId).ifPresent(lastRecord -> {
            if (ClockRecordType.IN.equals(lastRecord.getType())) {
                throw new BusinessRuleException("Cannot clock in twice in a row");
            }
        });
    }

    private void checkClockOutRules(Long userId) {
        clockRepo.findLatestByUserId(userId).ifPresent(lastRecord -> {
            if (ClockRecordType.OUT.equals(lastRecord.getType())) {
                throw new BusinessRuleException("Cannot clock out twice in a row");
            }
        });
    }
}
