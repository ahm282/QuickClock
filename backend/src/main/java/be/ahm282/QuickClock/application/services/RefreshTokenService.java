package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.TokenPairDTO;
import be.ahm282.QuickClock.application.ports.in.RefreshTokenUseCase;
import be.ahm282.QuickClock.application.ports.out.InvalidatedTokenRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.RefreshTokenRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.exception.TokenException;
import be.ahm282.QuickClock.domain.model.InvalidatedToken;
import be.ahm282.QuickClock.domain.model.RefreshToken;
import be.ahm282.QuickClock.domain.model.Role;
import be.ahm282.QuickClock.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final InvalidatedTokenRepositoryPort invalidatedTokenRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final TokenProviderPort tokenProviderPort;
    private final UserRepositoryPort userRepositoryPort;

    public RefreshTokenService(InvalidatedTokenRepositoryPort invalidatedTokenRepositoryPort,
                               RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                               TokenProviderPort tokenProviderPort,
                               UserRepositoryPort userRepositoryPort) {
        this.invalidatedTokenRepositoryPort = invalidatedTokenRepositoryPort;
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.tokenProviderPort = tokenProviderPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    @Transactional
    public TokenPairDTO rotateRefreshTokenByToken(String refreshToken) {
        if (!tokenProviderPort.isRefreshToken(refreshToken)) {
            throw new JwtException("Not a refresh token");
        }

        Claims claims = tokenProviderPort.parseClaims(refreshToken);
        String jti = claims.getId();
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();

        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for id " + userId));

        List<Role> roles = toRoleList(user);

        Optional<RefreshToken> existingTokenOpt = refreshTokenRepositoryPort.findByJti(jti);

        if (existingTokenOpt.isEmpty()) {
            log.warn("Refresh token not found in database for user {}, creating new family", username);
            return createNewTokenFamily(user, roles);
        }

        RefreshToken existingToken = existingTokenOpt.get();

        // Reuse detection
        if (existingToken.isRevoked() || existingToken.isUsed()) {
            log.error("SECURITY ALERT: Token reuse detected for user {} - Family: {}",
                    userId, existingToken.getRootFamilyId());

            refreshTokenRepositoryPort.revokeAllByFamilyId(existingToken.getRootFamilyId());

            throw new TokenException(
                    "Security Alert: Token reuse detected. All sessions in this family have been terminated.",
                    userId
            );
        }

        // Mark current token as used + revoked
        existingToken.setUsed(true);
        existingToken.setRevoked(true);
        refreshTokenRepositoryPort.save(existingToken);

        UUID parentId = UUID.fromString(jti);
        UUID rootFamilyId = existingToken.getRootFamilyId();

        return issueTokensInFamily(user, roles, rootFamilyId, parentId);
    }

    @Override
    @Transactional
    public void invalidateRefreshToken(String refreshToken) {
        tokenProviderPort.validateToken(refreshToken);
        Claims claims = tokenProviderPort.parseClaims(refreshToken);

        String jti = claims.getId();
        Long userId = claims.get("userId", Long.class);
        Instant expiry = claims.getExpiration().toInstant();

        invalidatedTokenRepositoryPort.save(new InvalidatedToken(jti, userId, expiry));

        refreshTokenRepositoryPort.findByJti(jti).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepositoryPort.save(token);
        });
    }

    @Override
    @Transactional
    public void invalidateAllTokensForUser(Long userId) {
        invalidatedTokenRepositoryPort.deleteByUserId(userId);
        refreshTokenRepositoryPort.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                invalidateRefreshToken(refreshToken);
            } catch (JwtException e) {
                log.warn("Invalid refresh token provided during logout: {}", e.getMessage());
            }
        }

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

    // ====================
    // Private helpers
    // ====================

    private List<Role> toRoleList(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        return List.copyOf(user.getRoles());
    }

    private TokenPairDTO createNewTokenFamily(User user, List<Role> roles) {
        UUID rootFamilyId = UUID.randomUUID();
        return issueTokensInFamily(user, roles, rootFamilyId, null);
    }

    /**
     * Core token-issuing logic for this service.
     * - Generates access + refresh
     * - Parses refresh JTI/expiry
     * - Persists refresh token with family/parent linkage
     */
    private TokenPairDTO issueTokensInFamily(User user,
                                             List<Role> roles,
                                             UUID rootFamilyId,
                                             UUID parentId) {

        Long userId = user.getId();
        String username = user.getUsername();
        String displayName = user.getDisplayName();
        String displayNameArabic = user.getDisplayNameArabic();

        String accessToken = tokenProviderPort.generateAccessToken(username, displayName, displayNameArabic, userId, roles);
        String refreshTokenJwt = tokenProviderPort.generateRefreshToken(username, displayName, displayNameArabic, userId);

        Claims claims = tokenProviderPort.parseClaims(refreshTokenJwt);
        String jti = claims.getId();
        Instant expiry = claims.getExpiration().toInstant();
        Instant issuedAt = Instant.now();

        RefreshToken newToken = new RefreshToken(
                jti,
                parentId,
                rootFamilyId,
                userId,
                false,
                false,
                issuedAt,
                expiry
        );
        refreshTokenRepositoryPort.save(newToken);

        log.debug("Issued new tokens for user {} - Family: {}, parent: {}",
                userId, rootFamilyId, parentId);

        return new TokenPairDTO(accessToken, refreshTokenJwt);
    }
}
