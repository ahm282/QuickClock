package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeResponseDTO;
import be.ahm282.QuickClock.application.ports.out.QRTokenPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QRCodeService {

    private final QRTokenPort qrTokenPort;
    private final UserRepositoryPort userRepositoryPort;

    public QRCodeService(QRTokenPort qrTokenPort, UserRepositoryPort userRepositoryPort) {
        this.qrTokenPort = qrTokenPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    /**
     * Generate a QR token URL for clocking IN
     */
    public ClockQRCodeResponseDTO generateClockInQRCode(UUID publicId) {
        User user = userRepositoryPort.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        String token = qrTokenPort.generateToken(user.getId(), "clock-in");
        String tokenId = qrTokenPort.extractTokenId(token);
        return new ClockQRCodeResponseDTO(
                token,
                "/clock/qr/in",
                tokenId
        );
    }

    /**
     * Generate a QR token URL for clocking OUT
     */
    public ClockQRCodeResponseDTO generateClockOutQRCode(UUID publicId) {
        User user = userRepositoryPort.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        String token = qrTokenPort.generateToken(user.getId(), "clock-out");
        String tokenId = qrTokenPort.extractTokenId(token);

        return new ClockQRCodeResponseDTO(
                token,
                "/clock/qr/out",
                tokenId
        );
    }
}
