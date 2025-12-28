package be.ahm282.QuickClock.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
        String username,

        @NotBlank
        @Size(min = 1, max = 72, message = "Password is required")
        String password
) {
}