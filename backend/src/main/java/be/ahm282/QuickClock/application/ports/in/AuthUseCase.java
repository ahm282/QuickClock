package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.application.dto.TokenPairDTO;

public interface AuthUseCase {
    TokenPairDTO login(String username, String password);
    Long register(String username, String password, String inviteCode);
}
