package be.ahm282.QuickClock.infrastructure.adapters.out.qr;

import be.ahm282.QuickClock.application.ports.out.QRTokenPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.infrastructure.adapters.in.qr.QRCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class QRTokenAdapter implements QRTokenPort {

    private final QRCodeGenerator generator;
    private final UserRepositoryPort userRepositoryPort;

    public QRTokenAdapter(QRCodeGenerator generator, UserRepositoryPort userRepositoryPort) {
        this.generator = generator;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public String generateToken(Long userId) {
        String secret = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"))
                .getSecret();
        return generator.generateToken(userId, secret);
    }

    @Override
    public Long validateAndExtractUserId(String token) {
        // Retrieve secret for user
        Long userIdFromToken = Long.parseLong(new String(java.util.Base64.getUrlDecoder().decode(token), java.nio.charset.StandardCharsets.UTF_8).split(":")[0]);
        String secret = userRepositoryPort.findById(userIdFromToken)
                .orElseThrow(() -> new BusinessRuleException("User not found"))
                .getSecret();

        return generator.validateAndExtractUserId(token, secret);
    }
}
