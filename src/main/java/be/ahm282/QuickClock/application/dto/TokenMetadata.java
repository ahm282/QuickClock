package be.ahm282.QuickClock.application.dto;

public record TokenMetadata(
        String deviceId,
        String ipAddress,
        String userAgent
) {}