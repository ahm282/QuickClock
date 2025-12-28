package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.application.dto.response.UserSummaryResponse;

import java.util.List;

public interface UserDirectoryUseCase {
    List<UserSummaryResponse> listEmployeesForKiosk();
}
