package be.ahm282.QuickClock.application.ports.in.dto;

public class ClockQRCodeResponseDTO {
    private String token;
    private String path;

    public ClockQRCodeResponseDTO(String token, String path) {
        this.token = token;
        this.path = path;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPath() {
        return path;
    }

    private  void setPath(String path) {
        this.path = path;
    }
}
