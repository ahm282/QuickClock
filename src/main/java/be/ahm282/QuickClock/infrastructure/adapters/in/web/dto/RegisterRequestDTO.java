package be.ahm282.QuickClock.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank
        @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, digits, dot, underscore and dash")
        String username,

        @NotBlank
        @Size(min = 10, max = 72, message = "Password must be between 10 and 72 characters")
        String password
) {
}
