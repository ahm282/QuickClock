package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.out.QRTokenPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QRCodeService {

    private final QRTokenPort qrTokenPort;
    private final UserRepositoryPort userRepositoryPort;
    private final String frontendBaseUrl;

    public QRCodeService(QRTokenPort qrTokenPort,
                         UserRepositoryPort userRepositoryPort,
                         @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.qrTokenPort = qrTokenPort;
        this.userRepositoryPort = userRepositoryPort;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * Generate a QR token URL for clocking IN
     */
    public ClockQRCodeResponseDTO generateClockInQRCode(Long userId) {
        String token = qrTokenPort.generateToken(userId);
        String url = frontendBaseUrl + "/api/clock/qr/in?token=" + token;

        return new ClockQRCodeResponseDTO(token, url);
    }

    /**
     * Optionally generate a QR token URL for clocking OUT
     */
    public ClockQRCodeResponseDTO generateClockOutQRCode(Long userId) {
        String token = qrTokenPort.generateToken(userId);
        String url = frontendBaseUrl + "/api/clock/qr/out?token=" + token;

        return new ClockQRCodeResponseDTO(token, url);
    }
}
