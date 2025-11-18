package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.application.dto.TokenMetadata;
import be.ahm282.QuickClock.application.dto.TokenPair;

public interface AuthUseCase {
    TokenPair login(String username, String password, TokenMetadata metadata);
    Long register(String username, String password);
}
