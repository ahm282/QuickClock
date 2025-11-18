package be.ahm282.QuickClock.infrastructure.security.service;

import be.ahm282.QuickClock.application.dto.TokenMetadata;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.jsonwebtoken.Jwts.SIG.HS512;

@Service
public class JwtTokenService implements TokenProviderPort {
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 1_800_000; // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 2_592_000_000L; // 30 days

    private final SecretKey signingKey;
    private final String issuer;
    private final String audience;
    private final JwtParser jwtParser;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);

        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("JWT secret key must be at least 64 bytes for HS512");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.audience = audience;

        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build();
    }

    public String generateAccessToken(String username, Long userId, TokenMetadata metadata) {
        return buildToken(username, userId, ACCESS_TOKEN_EXPIRATION_MS, "access", metadata);
    }

    public String generateRefreshToken(String username, Long userId, TokenMetadata metadata) {
        return buildToken(username, userId, REFRESH_TOKEN_EXPIRATION_MS, "refresh", metadata);
    }

    public String buildToken(String username, Long userId, long validityMs, String type, TokenMetadata metadata) {
        long now = System.currentTimeMillis();
        List<String> roles = List.of("EMPLOYEE");

        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .audience().add(audience).and()
                .claim("userId", userId)
                .claim("type", type)
                .claim("roles", roles)
                .claim("devideId", metadata.deviceId())
                .claim("ipAddress", metadata.ipAddress())
                .claim("userAgent", metadata.userAgent())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + validityMs))
                .signWith(signingKey, HS512)
                .compact();
    }

    @Override
    public void validateToken(String token) throws JwtException {
        jwtParser.parseSignedClaims(token);
    }

    public Claims parseClaims(String token) throws JwtException {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(parseClaims(token).get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public String extractJti(String token) {
        return parseClaims(token).get("jti", String.class);
    }

    public String extractAudience(String token) {
        return parseClaims(token).get("audience", String.class);
    }

    public Date extractIssuedAt(String token) {
        return parseClaims(token).getIssuedAt();
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }
}
