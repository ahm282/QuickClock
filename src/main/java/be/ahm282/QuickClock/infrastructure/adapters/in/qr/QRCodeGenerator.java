package be.ahm282.QuickClock.infrastructure.adapters.in.qr;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class QRCodeGenerator {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long INTERVAL_SECONDS = 30; // Rolling

    /**
     * Generate a rolling QR token for a user.
     * @param userId User ID
     * @param userSecret User-specific secret key (stored securely)
     * @return URL-safe Base64 token
     */
    public String generateToken(Long userId, String userSecret) {
        long timestamp = System.currentTimeMillis() / 1000 / INTERVAL_SECONDS;
        String payload = userId + ":" + timestamp;
        String signature = hmacSha256(userSecret, payload);
        String fullPayload = payload + ":" + signature;

        return Base64
                .getUrlEncoder().withoutPadding().encodeToString(fullPayload.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }
}
