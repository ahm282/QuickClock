package be.ahm282.QuickClock.infrastructure.adapters.out.qr;

import be.ahm282.QuickClock.application.ports.out.QRTokenPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.exception.ValidationException;
import be.ahm282.QuickClock.domain.model.QrTokenValidation;
import be.ahm282.QuickClock.domain.model.TokenValidationResult;
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
    public String generateToken(Long userId, String purpose) {
        String secret = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"))
                .getSecret();

        return secureTokenService.generateToken(userId, secret, purpose, null);
    }

    @Override
    public QrTokenValidation validate(String token, String expectedPurpose) {
        String secret = findUserSecretByToken(token);
        TokenValidationResult result =
                secureTokenService.validate(token, secret, expectedPurpose, null);

        return new QrTokenValidation(
                result.userId(),
                result.purpose(),
                result.tokenId()
        );
    }

    @Override
    public Long validateAndExtractUserId(String token, String purpose) {
        return validate(token, purpose).userId();
    }

    @Override
    public String extractTokenId(String token) {
        String decoded = secureTokenService.decodeToken(token);
        String[] parts = decoded.split("\\|");
        // version | userId | purpose | kioskId | iat | exp | jti | sig
        if (parts.length < 7) {
            throw new IllegalArgumentException("Token form illegal - insufficient parts: " + decoded);
        }
        return parts[6]; // tokenId
    }

    private String findUserSecretByToken(String token) {
        try {
            String decoded = secureTokenService.decodeToken(token);
            String[] parts = decoded.split("\\|", 3); // version, userId, ...

            if (parts.length < 2) {
                throw new IllegalArgumentException();
            }

            Long userId = Long.parseLong(parts[1]);

            return userRepositoryPort.findById(userId)
                    .orElseThrow(() -> new ValidationException("Token is expired or invalid"))
                    .getSecret();
        } catch (Exception e) {
            throw new ValidationException("Token is expired or invalid");
        }
    }
}
