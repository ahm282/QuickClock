package be.ahm282.QuickClock.application.dto.response;

public record ClockQRCodeResponse(
        String token,
        String path,
        String tokenId) {
}