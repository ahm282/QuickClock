package be.ahm282.QuickClock.infrastructure.adapters.in.web.dto;

import java.util.UUID;

public record UserSummaryDTO(UUID publicId, String displayName) {}
