package be.ahm282.QuickClock.application.dto;

import java.util.UUID;

public record UserSummaryView(UUID publicId, String displayName) {
}
