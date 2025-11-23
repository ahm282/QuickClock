package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeResponseDTO;
import be.ahm282.QuickClock.application.ports.out.QRTokenPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class QRCodeService {

    private final QRTokenPort qrTokenPort;

    public QRCodeService(QRTokenPort qrTokenPort, UserRepositoryPort userRepositoryPort) {
        this.qrTokenPort = qrTokenPort;
    }

    /**
     * Generate a QR token URL for clocking IN
     */
    public ClockQRCodeResponseDTO generateClockInQRCode(Long userId) {
        String token = qrTokenPort.generateToken(userId);
        return new ClockQRCodeResponseDTO(token, "/api/clock/qr/in");
    }

    /**
     * Generate a QR token URL for clocking OUT
     */
    public ClockQRCodeResponseDTO generateClockOutQRCode(Long userId) {
        String token = qrTokenPort.generateToken(userId);
        return new ClockQRCodeResponseDTO(token, "/api/clock/qr/out");
    }
}
