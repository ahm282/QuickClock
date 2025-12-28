package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.response.UserSummaryResponse;
import be.ahm282.QuickClock.application.ports.in.UserDirectoryUseCase;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDirectoryService implements UserDirectoryUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public UserDirectoryService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public List<UserSummaryResponse> listEmployeesForKiosk() {
        return userRepositoryPort.findAllActiveEmployees()
                .stream()
                .map(u -> new UserSummaryResponse(
                        u.getPublicId(),
                        u.getDisplayName(),
                        u.getDisplayNameArabic(),
                        null,  // Will be populated by controller
                        null   // Will be populated by controller
                ))
                .toList();
    }
}
