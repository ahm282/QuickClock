package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.in.ClockUseCase;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.exception.ValidationException;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.User;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.UserService;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class ClockService implements ClockUseCase {
    private final ClockRecordRepositoryPort repository;
    private final QRTokenService qrTokenService;
    private final UserService userService;

    public ClockService(ClockRecordRepositoryPort repository, QRTokenService qrTokenService, UserService userService) {
        this.repository = repository;
        this.qrTokenService = qrTokenService;
        this.userService = userService;
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

    public ClockRecord clockInWithQR(String token) {
        String userIdPart = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8).split(":")[0];
        Long userId = Long.parseLong(userIdPart);

        String secret = userService.getSecretForUser(userId);
        qrTokenService.validateAndExtractUserId(token, secret);

        List<ClockRecord> lastRecords = repository.findAllByUserId(userId);
        if (!lastRecords.isEmpty() && "IN".equals(lastRecords.getFirst().getType())) {
            throw new BusinessRuleException("Cannot clock in twice in a row.");
        }

        return repository.save(ClockRecord.create(userId, "IN"));
    }

    public ClockRecord clockOutWithQR(String token) {
        String userIdPart = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8).split(":")[0];
        Long userId = Long.parseLong(userIdPart);

        String secret = userService.getSecretForUser(userId);
        qrTokenService.validateAndExtractUserId(token, secret);

        List<ClockRecord> lastRecords = repository.findAllByUserId(userId);
        if (lastRecords.isEmpty() || "OUT".equals(lastRecords.getFirst().getType())) {
            throw new BusinessRuleException("Cannot clock out twice in a row.");
        }

        return repository.save(ClockRecord.create(userId, "OUT"));
    }
}
