package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.TokenPair;
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
    public TokenPair rotateRefreshTokenByToken(String refreshToken) {
        if (!tokenProviderPort.isRefreshToken(refreshToken)) {
            throw new JwtException("Not a refresh token");
        }

        Claims claims = tokenProviderPort.parseClaims(refreshToken);
        String jti = claims.getId();
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();

        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for id " + userId));

        Optional<RefreshToken> existingTokenOpt = refreshTokenRepositoryPort.findByJti(jti);

        if (existingTokenOpt.isEmpty()) {
            log.warn("Refresh token not found in database for user {}, creating new family", username);
            return createNewTokenFamily(user);
        }

        RefreshToken existingToken = existingTokenOpt.get();

        if (existingToken.isRevoked() || existingToken.isUsed()) {
            log.error("SECURITY ALERT: Token reuse detected for user {} - Family: {}",
                    userId, existingToken.getRootFamilyId());

            refreshTokenRepositoryPort.revokeAllByFamilyId(existingToken.getRootFamilyId());

            throw new TokenException(
                    "Security Alert: Token reuse detected. All sessions in this family have been terminated.",
                    userId
            );
        }

        // mark current token as used / revoked
        existingToken.setUsed(true);
        existingToken.setRevoked(true);
        refreshTokenRepositoryPort.save(existingToken);

        UUID parentId = UUID.fromString(jti);
        UUID rootFamilyId = existingToken.getRootFamilyId();

        TokenPair pair = issueNewTokens(user, rootFamilyId, parentId);
        log.debug("Token rotated successfully for user {} - Family: {}", userId, rootFamilyId);
        return pair;
    }

    /**
     * Creates a new token family for initial login or legacy refresh token.
     */
    private TokenPair createNewTokenFamily(User user) {
        UUID rootFamilyId = UUID.randomUUID();
        TokenPair pair = issueNewTokens(user, rootFamilyId, null);

        log.debug("New token family created for user {} - Family: {}", user.getId(), rootFamilyId);
        return pair;
    }

    /**
     * Shared logic for:
     *  - creating a new family (parentId == null)
     *  - rotating within an existing family (parentId != null)
     */
    private TokenPair issueNewTokens(User user, UUID rootFamilyId, UUID parentId) {
        Long userId = user.getId();
        String username = user.getUsername();

        Role role = user.getRole();
        List<Role> roles = (role != null) ? List.of(role) : List.of();

        String accessToken = tokenProviderPort.generateAccessToken(username, userId, roles);
        String refreshTokenJwt = tokenProviderPort.generateRefreshToken(username, userId);

        Claims claims = tokenProviderPort.parseClaims(refreshTokenJwt);
        String jti = claims.getId();
        Instant expiry = claims.getExpiration().toInstant();
        Instant issuedAt = Instant.now();

        RefreshToken token = new RefreshToken(
                jti,
                parentId,      // null => root, non-null => child
                rootFamilyId,
                userId,
                false,
                false,
                issuedAt,
                expiry
        );
        refreshTokenRepositoryPort.save(token);

        return new TokenPair(accessToken, refreshTokenJwt);
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
}
