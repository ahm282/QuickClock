package be.ahm282.QuickClock.infrastructure.adapters.in.qr;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class QRCodeGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long INTERVAL_SECONDS = 30;

    public String generateToken(Long userId, String userSecret) {
        long timestamp = System.currentTimeMillis() / 1000 / INTERVAL_SECONDS;
        String payload = userId + ":" + timestamp;
        String signature = hmacSha256(userSecret, payload);
        String fullPayload = payload + ":" + signature;

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(fullPayload.getBytes(StandardCharsets.UTF_8));
    }

    public Long validateAndExtractUserId(String token, String userSecret) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid token format");

            long userId = Long.parseLong(parts[0]);
            long timestamp = Long.parseLong(parts[1]);
            String signature = parts[2];

            String expectedSignature = hmacSha256(userSecret, parts[0] + ":" + parts[1]);
            if (!expectedSignature.equals(signature)) throw new IllegalArgumentException("Invalid token signature");

            long currentWindow = System.currentTimeMillis() / 1000 / INTERVAL_SECONDS;
            if (Math.abs(currentWindow - timestamp) > 1) throw new IllegalArgumentException("Token expired");

            return userId;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    private String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }
}
