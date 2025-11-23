package be.ahm282.QuickClock.infrastructure.security.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class SecureTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA512";
    private static final long TOKEN_TTL_SECONDS = 30L;

    public String generateToken(Long userId, String userSecret) {
        long issuedAt = Instant.now().getEpochSecond();
        String payload = userId + ":" + issuedAt;
        String signature = hmac(userSecret, payload);
        String fullPayload = payload + ":" + signature;

        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(fullPayload.getBytes(StandardCharsets.UTF_8));
    }

    public Long extractUserIdUnsafe(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length < 2) {
                throw new IllegalArgumentException();
            }
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

            String userIdPart = parts[0];
            long issuedAt = Long.parseLong(parts[1]);
            String providedSignature = parts[2];

            long now = Instant.now().getEpochSecond();
            long age = now - issuedAt;

            if (age < 0 || age > TOKEN_TTL_SECONDS) {
                return false; // Token expired or clock skew detected
            }

            String expectedSignature = hmac(userSecret, userIdPart + ":" + issuedAt);
            byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] providedBytes = providedSignature.getBytes(StandardCharsets.UTF_8);

            return MessageDigest.isEqual(expectedBytes, providedBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private String hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC generation failed", e);
        }
    }
}
