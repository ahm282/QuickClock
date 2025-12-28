package be.ahm282.QuickClock.application.ports.in.dto;

public record ClockQRCodeResponseDTO(
        String token,
        String path,
        String tokenId) {
}