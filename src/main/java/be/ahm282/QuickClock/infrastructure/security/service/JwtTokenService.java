package be.ahm282.QuickClock.infrastructure.security.service;

import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.jsonwebtoken.Jwts.SIG.HS512;

@Service
public class JwtTokenService implements TokenProviderPort {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    private static final long ACCESS_TOKEN_EXPIRATION_MS = Duration.ofMinutes(30).getSeconds(); // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRATION_MS = Duration.ofDays(14).getSeconds(); // 14 days

    private final SecretKey signingKey;
    private final String issuer;
    private final String audience;
    private final JwtParser jwtParser;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience
    ) {
        if (base64Secret == null || base64Secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable must be set! " +
                            "Generate one with: openssl rand -base64 64"
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(base64Secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "JWT_SECRET is not valid Base64! " +
                            "Generate a new one with: openssl rand -base64 64",
                    e
            );
        }

        if (keyBytes.length < 64) {
            throw new IllegalArgumentException(
                    "JWT secret key must be at least 64 bytes for HS512. " +
                            "Current length: " + keyBytes.length + " bytes. " +
                            "Generate a new one with: openssl rand -base64 64"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.audience = audience;

        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build();

        // Log configuration (without exposing the secret!)
        log.info("JWT Configuration loaded - Issuer: {}, Audience: {}",
                issuer, audience);
    }

    @Override
    public String generateAccessToken(String username, Long userId, List<Role> roles) {
        return buildToken(username, userId, ACCESS_TOKEN_EXPIRATION_MS, "access", roles);
    }

    @Override
    public String generateRefreshToken(String username, Long userId) {
        return buildToken(username, userId, REFRESH_TOKEN_EXPIRATION_MS, "refresh", null);
    }

    private String buildToken(String username, Long userId, long validityMs, String type, List<Role> roles) {
        long now = System.currentTimeMillis();

        var builder = Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .audience().add(audience).and()
                .claim("userId", userId)
                .claim("type", type)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + validityMs))
                .signWith(signingKey, HS512);

        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles.stream().map(Role::name).toList());
        }

        return builder.compact();
    }

    @Override
    public void validateToken(String token) throws JwtException {
        jwtParser.parseSignedClaims(token);
    }

    @Override
    public Claims parseClaims(String token) throws JwtException {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    @Override
    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(parseClaims(token).get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    @Override
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    @Override
    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    @Override
    public String extractAudience(String token) {
        // Audience is a Set in JWT, get the first one
        return parseClaims(token).getAudience().iterator().next();
    }

    @Override
    public Date extractIssuedAt(String token) {
        return parseClaims(token).getIssuedAt();
    }

    @Override
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }
}