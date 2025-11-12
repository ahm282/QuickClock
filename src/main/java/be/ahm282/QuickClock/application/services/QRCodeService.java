package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeResponseDTO;
import be.ahm282.QuickClock.domain.exception.NotFoundException;
import be.ahm282.QuickClock.domain.model.User;
import be.ahm282.QuickClock.infrastructure.adapters.in.qr.QRCodeGenerator;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.UserRepository;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QRCodeService {
    private final QRCodeGenerator qrCodeGenerator;
    private final String frontendBaseUrl;
    private final UserRepository userRepository;

    public QRCodeService(QRCodeGenerator qrCodeGenerator, @Value("${app.frontend.base-url}") String frontendBaseUrl, UserRepository userRepository) {
        this.qrCodeGenerator = qrCodeGenerator;
        this.frontendBaseUrl = frontendBaseUrl;
        this.userRepository = userRepository;
    }

    // TODO: Generate OUT code as well
    public ClockQRCodeResponseDTO generateQRCode(Long userId) {
        User user = UserMapper.toDomain(userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found")));
        String token = qrCodeGenerator.generateToken(user.getId(), user.getSecret());
        String url = frontendBaseUrl + "/api/clock/qr/in?token=" + token;
        return new ClockQRCodeResponseDTO(token, url);
    }
}
