package be.ahm282.QuickClock.application.ports.in.dto;

import jakarta.validation.constraints.NotBlank;

public class ClockQRCodeRequestDTO {
    @NotBlank(message = "QR Token is required")
    private String token;

    public ClockQRCodeRequestDTO() {}

    public ClockQRCodeRequestDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
