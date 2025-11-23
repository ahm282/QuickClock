package be.ahm282.QuickClock.infrastructure.adapters.out.qr;

import be.ahm282.QuickClock.application.ports.out.QRTokenPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.exception.ValidationException;
import be.ahm282.QuickClock.infrastructure.security.service.SecureTokenService;
import org.springframework.stereotype.Component;

@Component
public class QRTokenAdapter implements QRTokenPort {

    private final SecureTokenService secureTokenService;
    private final UserRepositoryPort userRepositoryPort;

    public QRTokenAdapter(SecureTokenService secureTokenService, UserRepositoryPort userRepositoryPort) {
        this.secureTokenService = secureTokenService;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public String generateToken(Long userId) {
        String secret = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"))
                .getSecret();

        return secureTokenService.generateToken(userId, secret);
    }

    @Override
    public Long validateAndExtractUserId(String token) {
        Long userId;

        try {
            userId = secureTokenService.extractUserIdUnsafe(token);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Token is expired or invalid");
        }

        String secret = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ValidationException("Token is expired or invalid"))
                .getSecret();

        if (!secureTokenService.isValid(token, secret)) {
            throw new ValidationException("Token is expired or invalid");
        }

        return userId;
    }
}
