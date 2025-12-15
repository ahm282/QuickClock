package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.ports.in.UserDirectoryUseCase;
import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.UserSummaryDTO;
import be.ahm282.QuickClock.infrastructure.security.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public List<UserSummaryDTO> listEmployees() {
        securityUtil.requireKioskOrAdmin();

        return userDirectoryUseCase.listEmployeesForKiosk()
                .stream()
                .map(u -> {
                    var user = userRepository.findByPublicId(u.publicId())
                            .orElseThrow();
                    var lastClock = clockRecordRepository.findLatestByUserId(user.getId());
                    String lastClockType = lastClock.map(c -> c.getType().name()).orElse(null);
                    String lastClockTime = lastClock.map(c -> c.getRecordedAt().toString()).orElse(null);
                    return new UserSummaryDTO(u.publicId(), u.displayName(), lastClockType, lastClockTime);
                })
                .toList();
    }
}
