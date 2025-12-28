package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.dto.response.UserSummaryResponse;
import be.ahm282.QuickClock.application.ports.in.UserDirectoryUseCase;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.security.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/kiosk")
public class KioskController {
    private final UserDirectoryUseCase userDirectoryUseCase;
    private final SecurityUtil securityUtil;
    private final ClockRecordRepositoryPort clockRecordRepository;
    private final UserRepositoryPort userRepository;

    public KioskController(UserDirectoryUseCase userDirectoryUseCase,
                          SecurityUtil securityUtil,
                          ClockRecordRepositoryPort clockRecordRepository,
                          UserRepositoryPort userRepository) {
        this.userDirectoryUseCase = userDirectoryUseCase;
        this.securityUtil = securityUtil;
        this.clockRecordRepository = clockRecordRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/employees")
    public List<UserSummaryResponse> listEmployees() {
        securityUtil.requireKioskOrAdmin();

        return userDirectoryUseCase.listEmployeesForKiosk()
                .stream()
                .map(u -> {
                    var user = userRepository.findByPublicId(u.publicId())
                            .orElseThrow();
                    var lastClock = clockRecordRepository.findLatestByUserId(user.getId());
                    String lastClockType = lastClock.map(c -> c.getType().name()).orElse(null);
                    Instant lastClockTime = lastClock.map(ClockRecord::getRecordedAt).orElse(null);
                    return new UserSummaryResponse(u.publicId(), u.displayName(), u.displayNameArabic(), lastClockType, lastClockTime);
                })
                .toList();
    }
}
