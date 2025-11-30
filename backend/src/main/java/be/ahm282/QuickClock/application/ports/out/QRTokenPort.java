package be.ahm282.QuickClock.application.ports.out;

public interface QRTokenPort {
    String generateToken(Long userId, String purpose);
    Long validateAndExtractUserId(String token, String purpose);
}
