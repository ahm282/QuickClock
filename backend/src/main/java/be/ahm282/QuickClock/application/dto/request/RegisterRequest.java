package be.ahm282.QuickClock.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "Username can only contain letters, digits, dot, underscore and dash")
        String username,

        @NotBlank
        @Size(min = 3, max = 32, message = "Display name must be between 3 and 32 characters")
        String displayName,

        @NotBlank
        @Size(min = 3, max = 32, message = "Arabic display name must be between 3 and 32 characters")
        String displayNameArabic,

        @NotBlank
        @Size(min = 10, max = 72, message = "Password must be between 10 and 72 characters")
        @Pattern(
                regexp = "^[\\x21-\\x7E]+$",
                message = "Password may only contain printable ASCII characters (no spaces or emojis)")
        String password,

        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid invite code format")
        String inviteCode
) {
}
