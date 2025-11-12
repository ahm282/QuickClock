package be.ahm282.QuickClock.application.ports.in.dto;

public class ClockQRCodeResponseDTO {
    private String token;
    private String url;

    public ClockQRCodeResponseDTO() {}

    public ClockQRCodeResponseDTO(String token, String url) {
        this.token = token;
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
