package be.ahm282.QuickClock.infrastructure.security;

import be.ahm282.QuickClock.domain.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static io.jsonwebtoken.Jwts.SIG.HS512;

@Service
public class JwtTokenService {
    private static final long ACCESS_TOKEN_EXPIRATION = 1_800_000; // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604_800_000L; // 7 days
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

        if (keyBytes.length < 64) { // <<< ADDED: Enforce key length for HS512
            throw new IllegalArgumentException("JWT secret key must be at least 64 bytes (512 bits) for HS512.");
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

    public String generateAccessToken(String username, Long userId) {
        return buildToken(username, userId, ACCESS_TOKEN_EXPIRATION, "access");
    }

    public String generateRefreshToken(String username, Long userId) {
        return buildToken(username, userId, REFRESH_TOKEN_EXPIRATION, "refresh");
    }

    public String buildToken(String username, Long userId, long expiration, String type) {
        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .audience().add(audience).and()
                .claim("userId", userId)
                .claim("type", type)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey, HS512)
                .compact();
    }

    public Claims parseToken(String token) {
        return jwtParser.parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(parseToken(token).get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }
}
