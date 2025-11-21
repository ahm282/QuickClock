package be.ahm282.QuickClock.application.ports.out;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import java.util.Date;

public interface TokenProviderPort {
    String generateAccessToken(String username, Long userId);
    String generateRefreshToken(String username, Long userId);

    boolean isRefreshToken(String token);
    void validateToken(String token) throws JwtException;
    Claims parseClaims(String token);

    String extractJti(String token);
    String extractUsername(String token);
    Long extractUserId(String token);
    String extractAudience(String token);
    Date extractIssuedAt(String token);
    Date extractExpiration(String token);
}
