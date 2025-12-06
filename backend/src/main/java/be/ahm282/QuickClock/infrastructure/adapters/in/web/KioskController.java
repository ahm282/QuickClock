package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.ports.in.UserDirectoryUseCase;
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

    public KioskController(UserDirectoryUseCase userDirectoryUseCase, SecurityUtil securityUtil) {
        this.userDirectoryUseCase = userDirectoryUseCase;
        this.securityUtil = securityUtil;
    }

    @GetMapping("/users")
    public List<UserSummaryDTO> listEmployees() {
        securityUtil.requireKioskOrAdmin();

        return userDirectoryUseCase.listEmployeesForKiosk()
                .stream()
                .map(u -> new UserSummaryDTO(u.publicId(), u.displayName()))
                .toList();
    }
}
