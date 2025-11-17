package be.ahm282.QuickClock.application.ports.out;

public interface QRTokenPort {
    String generateToken(Long userId);
    Long validateAndExtractUserId(String token);
}
