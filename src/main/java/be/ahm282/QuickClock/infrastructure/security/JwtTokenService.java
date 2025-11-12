package be.ahm282.QuickClock.infrastructure.security;

import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtTokenService {
    private static final long ACCESS_TOKEN_EXPIRATION = 1_800_000; // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 2_592_000_000L; // 7 days
    private final SecretKey signingKey;

    public JwtTokenService(@Value("${app.jwt.secret}") String base64Secret) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserEntity user) {
        return generateAccessToken(user);
    }

    public String generateAccessToken(UserEntity user) {
        return buildToken(user, ACCESS_TOKEN_EXPIRATION, "access");
    }

    public String generateRefreshToken(UserEntity user) {
        return buildToken(user, REFRESH_TOKEN_EXPIRATION, "refresh");
    }

    public String buildToken(UserEntity user, long expiration, String type) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseToken(token).get("type", String.class));
    }

    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }
}
