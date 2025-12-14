package be.ahm282.QuickClock.application.ports.out;

import be.ahm282.QuickClock.domain.model.QrTokenValidation;

public interface QRTokenPort {
    String generateToken(Long userId, String purpose);
    Long validateAndExtractUserId(String token, String purpose);
    QrTokenValidation validate(String token, String expectedPurpose);
    String extractTokenId(String token);
}
