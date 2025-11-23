package be.ahm282.QuickClock.infrastructure.security.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class SecureTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA512";
    private static final long INTERVAL_SECONDS = 30;

    public String generateToken(Long userId, String userSecret) {
        long timestamp = getCurrentWindow();
        String payload = userId + ":" + timestamp;
        String signature = hmacSha512(userSecret, payload);
        String fullPayload = payload + ":" + signature;

        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(fullPayload.getBytes(StandardCharsets.UTF_8));
    }

    public Long extractUserIdUnsafe(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length < 2) throw new IllegalArgumentException();
            return Long.parseLong(parts[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed token format");
        }
    }

    public boolean isValid(String token, String userSecret) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 3) {
                return false;
            }

            long timestamp = Long.parseLong(parts[1]);
            String signature = parts[2];

            String expectedSignature = hmacSha512(userSecret, parts[0] + ":" + parts[1]);
            boolean signaturesMatch = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );

            if (!signaturesMatch) {
                return false;
            }

            long currentWindow = getCurrentWindow();
            return Math.abs(currentWindow - timestamp) <= 1;
        } catch (Exception e) {
            return false;
        }
    }

    private long getCurrentWindow() {
        return System.currentTimeMillis() / 1000 / INTERVAL_SECONDS;
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC generation failed", e);
        }
    }
}
