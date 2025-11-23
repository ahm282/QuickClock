package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.RefreshTokenUseCase;
import be.ahm282.QuickClock.application.ports.out.InvalidatedTokenRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.RefreshTokenRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.domain.exception.TokenException;
import be.ahm282.QuickClock.domain.model.InvalidatedToken;
import be.ahm282.QuickClock.domain.model.RefreshToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh token service with strict token rotation and family-based reuse detection.
 * Implements automatic session invalidation on token theft.
 */
@Service
public class RefreshTokenService implements RefreshTokenUseCase {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final InvalidatedTokenRepositoryPort invalidatedTokenRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final TokenProviderPort tokenProviderPort;

    public RefreshTokenService(InvalidatedTokenRepositoryPort invalidatedTokenRepositoryPort,
                               RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                               TokenProviderPort tokenProviderPort) {
        this.invalidatedTokenRepositoryPort = invalidatedTokenRepositoryPort;
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.tokenProviderPort = tokenProviderPort;
    }

    @Override
    @Transactional
    public TokenPair rotateRefreshTokenByToken(String refreshToken) {
        // 1. Validate it's a refresh token
        boolean isRefreshToken = tokenProviderPort.isRefreshToken(refreshToken);
        if (!isRefreshToken) {
            throw new JwtException("Not a refresh token");
        }

        // 2. Parse token claims
        Claims claims = tokenProviderPort.parseClaims(refreshToken);
        String jti = claims.getId();
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();

        // 3. Check if this token exists in our database
        Optional<RefreshToken> existingTokenOpt = refreshTokenRepositoryPort.findByJti(jti);

        if (existingTokenOpt.isEmpty()) {
            // Token not in database - this could be a first-time login or token from before family tracking
            log.warn("Refresh token not found in database for user {}, creating new family", userId);
            return createNewTokenFamily(username, userId);
        }

        RefreshToken existingToken = existingTokenOpt.get();

        // 4. SECURITY: Reuse Detection - The core of the protection
        if (existingToken.isRevoked() || existingToken.isUsed()) {
            log.error("SECURITY ALERT: Token reuse detected for user {} - Family: {}",
                     userId, existingToken.getRootFamilyId());

            // Kill the entire token family
            refreshTokenRepositoryPort.revokeAllByFamilyId(existingToken.getRootFamilyId());

            throw new TokenException(
                "Security Alert: Token reuse detected. All sessions in this family have been terminated.",
                userId
            );
        }

        // 5. Mark current token as used/revoked
        existingToken.setUsed(true);
        existingToken.setRevoked(true);
        refreshTokenRepositoryPort.save(existingToken);

        // 6. Create new token in the same family
        UUID parentId = UUID.fromString(jti);
        UUID rootFamilyId = existingToken.getRootFamilyId();

        // Generate new JWT tokens
        String newAccessToken = tokenProviderPort.generateAccessToken(username, userId);
        String newRefreshTokenJwt = tokenProviderPort.generateRefreshToken(username, userId);

        // Parse new refresh token to get its JTI
        Claims newClaims = tokenProviderPort.parseClaims(newRefreshTokenJwt);
        String newJti = newClaims.getId();
        Instant newExpiry = newClaims.getExpiration().toInstant();
        Instant issuedAt = Instant.now();

        // Store new refresh token in database
        RefreshToken newToken = new RefreshToken(
            newJti,
            parentId,
            rootFamilyId,
            userId,
            false,
            false,
            issuedAt,
            newExpiry
        );
        refreshTokenRepositoryPort.save(newToken);

        log.debug("Token rotated successfully for user {} - Family: {}", userId, rootFamilyId);

        return new TokenPair(newAccessToken, newRefreshTokenJwt);
    }

    /**
     * Creates a new token family for initial login.
     */
    private TokenPair createNewTokenFamily(String username, Long userId) {
        UUID rootFamilyId = UUID.randomUUID();

        // Generate new JWT tokens
        String newAccessToken = tokenProviderPort.generateAccessToken(username, userId);
        String newRefreshTokenJwt = tokenProviderPort.generateRefreshToken(username, userId);

        // Parse refresh token to get its JTI
        Claims claims = tokenProviderPort.parseClaims(newRefreshTokenJwt);
        String jti = claims.getId();
        Instant expiry = claims.getExpiration().toInstant();
        Instant issuedAt = Instant.now();

        // Store refresh token as root of new family (no parent)
        RefreshToken newToken = new RefreshToken(
            jti,
            null,  // No parent - this is the root
            rootFamilyId,
            userId,
            false,
            false,
            issuedAt,
            expiry
        );
        refreshTokenRepositoryPort.save(newToken);

        log.debug("New token family created for user {} - Family: {}", userId, rootFamilyId);

        return new TokenPair(newAccessToken, newRefreshTokenJwt);
    }

    @Override
    @Transactional
    public void invalidateRefreshToken(String refreshToken) {
        tokenProviderPort.validateToken(refreshToken);
        Claims claims = tokenProviderPort.parseClaims(refreshToken);

        String jti = claims.getId();
        Long userId = claims.get("userId", Long.class);
        Instant expiry = claims.getExpiration().toInstant();

        // Mark as invalidated in both systems
        invalidatedTokenRepositoryPort.save(new InvalidatedToken(jti, userId, expiry));

        // Also mark as revoked in refresh token repository if it exists
        refreshTokenRepositoryPort.findByJti(jti).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepositoryPort.save(token);
        });
    }

    @Override
    @Transactional
    public void invalidateAllTokensForUser(Long userId) {
        // Clean up both repositories
        invalidatedTokenRepositoryPort.deleteByUserId(userId);
        refreshTokenRepositoryPort.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // Invalidate the refresh token if present
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                invalidateRefreshToken(refreshToken);
            } catch (JwtException e) {
                log.warn("Invalid refresh token provided during logout: {}", e.getMessage());
            }
        }

        // Blacklist the access token if present and valid
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                tokenProviderPort.validateToken(accessToken);

                if (!tokenProviderPort.isRefreshToken(accessToken)) {
                    Claims claims = tokenProviderPort.parseClaims(accessToken);
                    String jti = claims.getId();
                    Long userId = claims.get("userId", Long.class);
                    Instant expiry = claims.getExpiration().toInstant();

                    if (jti != null && userId != null && expiry != null) {
                        invalidatedTokenRepositoryPort.save(new InvalidatedToken(jti, userId, expiry));
                        log.debug("Access token blacklisted during logout: userId={}, jti={}", userId, jti);
                    }
                } else {
                    log.warn("Access token provided during logout is actually a refresh token, skipping blacklist.");
                }
            } catch (JwtException e) {
                log.warn("Invalid access token provided during logout: {}", e.getMessage());
            }
        }
    }
}
