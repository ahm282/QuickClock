package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.application.dto.TokenPair;

public interface AuthUseCase {
    TokenPair login(String username, String password);
    Long register(String username, String password, String inviteCode);
}
