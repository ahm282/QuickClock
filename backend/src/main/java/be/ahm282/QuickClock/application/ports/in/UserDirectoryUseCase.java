package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.application.dto.UserSummaryView;

import java.util.List;

public interface UserDirectoryUseCase {
    List<UserSummaryView> listEmployeesForKiosk();
}
