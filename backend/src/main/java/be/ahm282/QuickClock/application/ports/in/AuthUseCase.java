package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.application.dto.TokenPairDTO;

public interface AuthUseCase {
    TokenPairDTO login(String username, String password);
    void register(String username, String displayName, String displayNameArabic, String password, String inviteCode);
}
