package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class QRTokenService {
    private static final long INTERVAL_SECONDS = 30;

    /**
     * Validate a QR token and return the associated user ID.
     * @param token Base64 URL-safe token
     * @param userSecret Secret key for the user
     * @return User ID
     */
    public Long validateAndExtractUserId(String token , String userSecret) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");

            if (parts.length != 3) {
                throw new BusinessRuleException("Invalid token");
            }

            Long userId = Long.parseLong(parts[0]);
            Long timestamp = Long.parseLong(parts[1]);
            String signature = parts[2];

            String expectedSignature = hmacSha256(userSecret, userId + ":" + timestamp);
            if (!expectedSignature.equals(signature)) {
                throw new BusinessRuleException("Invalid token signature");
            }

            Long nowWindow = System.currentTimeMillis() / 1000 /  INTERVAL_SECONDS;
            if (Math.abs(nowWindow - timestamp) >= 1) {
                throw new BusinessRuleException("Token expired");
            }

            return userId;
        } catch (Exception e) {
            throw new BusinessRuleException("Invalid token");
        }
    }

    private String hmacSha256(String secret, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }
}
