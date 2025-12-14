package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.in.ClockUseCase;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.QRTokenPort;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    public ClockRecord clockInWithQR(String qrToken, Long authenticatedUserId) {
        var validation = qrTokenPort.validate(qrToken, "clock-in");
        Long tokenUserId = validation.userId();

        if (!tokenUserId.equals(authenticatedUserId)) {
            throw new BusinessRuleException("QR token does not belong to the authenticated user");
        }

        return clockIn(tokenUserId);
    }

    @Override
    public ClockRecord clockOutWithQR(String qrToken, Long authenticatedUserId) {
        var validation = qrTokenPort.validate(qrToken, "clock-out");
        Long tokenUserId = validation.userId();

        if (!tokenUserId.equals(authenticatedUserId)) {
            throw new BusinessRuleException("QR token does not belong to the authenticated user");
        }

        // tokenId available here soon for SSE:
        // String tokenId = validation.tokenId();

        return clockOut(tokenUserId);
    }

    @Override
    public List<ClockRecord> getHistory(Long userId) {
        return clockRepo.findAllByUserId(userId);
    }

    // ---------- Admin manual clocking ----------

    @Override
    public ClockRecord adminClockIn(Long userId, Instant recordedAtTimestamp, String reason) {
        // TODO: Consider enforcing time constraints (e.g., cannot clock in for a time in the future)
        checkClockInRules(userId);
        ClockRecord record = ClockRecord.createAt(userId, ClockRecordType.IN, recordedAtTimestamp, reason);
        return clockRepo.save(record);
    }

    @Override
    public ClockRecord adminClockOut(Long userId, Instant recordedAtTimestamp, String reason) {
        checkClockOutRules(userId);
        ClockRecord record = ClockRecord.createAt(userId, ClockRecordType.OUT, recordedAtTimestamp, reason);
        return clockRepo.save(record);
    }

    // ---------- Business rules ----------
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
