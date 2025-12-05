package be.ahm282.QuickClock.infrastructure.security.service;

import be.ahm282.QuickClock.domain.model.TokenValidationResult;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecureTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA512";
    private static final String TOKEN_VERSION = "v1";
    private static final String FIELD_SEPARATOR = "|";

    private static final long TOKEN_TTL_SECONDS = 30L;
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 5L;
    private static final int TOKEN_ID_BYTES = 16;
    private static final int MAX_TOKEN_BYTES = 512;
    private static final int EXPECTED_FIELD_COUNT = 8;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Long> usedTokenIds = new ConcurrentHashMap<>();

    public String generateToken(Long userId, String userSecret, String purpose, String kioskId) {
        long iat = currentEpochSeconds();
        long exp = iat + TOKEN_TTL_SECONDS;
        String jti = randomBase64Url();

        String payload = String.join(FIELD_SEPARATOR,
                TOKEN_VERSION,
                Long.toString(userId),
                nullToEmpty(purpose),
                nullToEmpty(kioskId),
                Long.toString(iat),
                Long.toString(exp),
                jti
        );

        String signature = hmac(userSecret, payload);
        String token = payload + FIELD_SEPARATOR + signature;

        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public TokenValidationResult validate(String token, String userSecret, String expectedPurpose, String expectedKioskId) {
        String decoded = decodeToken(token);
        String[] parts = decoded.split("\\|", EXPECTED_FIELD_COUNT);
        if (parts.length != EXPECTED_FIELD_COUNT) {
            throw new IllegalArgumentException("Malformed token");
        }

        String version = parts[0];
        if (!TOKEN_VERSION.equals(version)) {
            throw new IllegalArgumentException("Unsupported token version");
        }

        long userId = parseLong(parts[1], "userId");
        String purpose = parts[2];
        String kioskId = emptyToNull(parts[3]);
        long issuedAt = parseLong(parts[4], "issuedAt");
        long expiresAt = parseLong(parts[5], "expiresAt");
        String tokenId = parts[6];
        String providedSignature = parts[7];

        long now = currentEpochSeconds();
        if (now + ALLOWED_CLOCK_SKEW_SECONDS < issuedAt || now - ALLOWED_CLOCK_SKEW_SECONDS > expiresAt) {
            throw new IllegalArgumentException("Token expired or not valid");
        }

        String payload = String.join(FIELD_SEPARATOR, version, Long.toString(userId), purpose,
                nullToEmpty(kioskId), Long.toString(issuedAt), Long.toString(expiresAt), tokenId);
        String expectedSig = hmac(userSecret, payload);

        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Bad signature");
        }

        if (expectedPurpose != null && !Objects.equals(purpose, expectedPurpose)) {
            throw new IllegalArgumentException("Purpose mismatch");
        }
        if (expectedKioskId != null && !Objects.equals(kioskId, expectedKioskId)) {
            throw new IllegalArgumentException("Kiosk mismatch");
        }

        // Single-use check (atomic)
        if (!markTokenIdAsUsed(tokenId, expiresAt)) {
            throw new IllegalArgumentException("Token already used");
        }

        cleanUpExpiredTokens(now);

        return new TokenValidationResult(userId, purpose, kioskId,
                Instant.ofEpochSecond(issuedAt),
                Instant.ofEpochSecond(expiresAt),
                tokenId);
    }

    // ----------------------- helpers -----------------------

    public String decodeToken(String token) {
        byte[] raw = Base64.getUrlDecoder().decode(token);

        if (raw.length > MAX_TOKEN_BYTES) {
            throw new IllegalArgumentException("Token too large");
        }

        return new String(raw, StandardCharsets.UTF_8);
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

    private String randomBase64Url() {
        byte[] bytes = new byte[SecureTokenService.TOKEN_ID_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean markTokenIdAsUsed(String tokenId, long expiresAt) {
        // returns true if first use, false if already present
        return usedTokenIds.putIfAbsent(tokenId, expiresAt) == null;
    }

    private void cleanUpExpiredTokens(long now) {
        usedTokenIds.entrySet().removeIf(e -> e.getValue() + ALLOWED_CLOCK_SKEW_SECONDS < now);
    }

    private static long currentEpochSeconds() { return Instant.now().getEpochSecond(); }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String emptyToNull(String s) { return s == null || s.isEmpty() ? null : s; }

    private static long parseLong(String s, String field) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed " + field);
        }
    }
}
