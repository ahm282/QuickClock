package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.application.dto.response.TokenPairResponse;

public interface AuthUseCase {
    TokenPairResponse login(String username, String password);
    void register(String username, String displayName, String displayNameArabic, String password, String inviteCode);
}
