package be.ahm282.QuickClock.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ClockQRCodeRequest {
    @NotBlank(message = "QR Token is required")
    private String token;

    public ClockQRCodeRequest() {}

    public ClockQRCodeRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
