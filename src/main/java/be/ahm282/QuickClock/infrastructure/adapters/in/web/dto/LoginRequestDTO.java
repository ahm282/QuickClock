package be.ahm282.QuickClock.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank
        @Size(min = 3, max = 32)
        String username,

        @NotBlank
        @Size(min = 3, max = 72)
        String password
) {
}