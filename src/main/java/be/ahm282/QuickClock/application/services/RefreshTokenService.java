package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.dto.TokenMetadata;
import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.RefreshTokenUseCase;
import be.ahm282.QuickClock.application.ports.out.InvalidatedTokenRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.exception.TokenException;
import be.ahm282.QuickClock.domain.model.InvalidatedToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {
    private final InvalidatedTokenRepositoryPort invalidatedTokenRepositoryPort;
    private final TokenProviderPort tokenProviderPort;

    public RefreshTokenService(InvalidatedTokenRepositoryPort invalidatedTokenRepositoryPort,
                               TokenProviderPort tokenProviderPort) {
        this.invalidatedTokenRepositoryPort = invalidatedTokenRepositoryPort;
        this.tokenProviderPort = tokenProviderPort;
    }

    @Override
    @Transactional
    public TokenPair rotateRefreshTokenByToken(String refreshToken, TokenMetadata metadata) {
        boolean isRefreshToken = tokenProviderPort.isRefreshToken(refreshToken);

        if (!isRefreshToken) {
            throw new JwtException("Not a refresh token");
        }

        String jti = tokenProviderPort.extractJti(refreshToken);
        Long userId = tokenProviderPort.extractUserId(refreshToken);
        String username =  tokenProviderPort.extractUsername(refreshToken);
        Instant expiry = tokenProviderPort.extractExpiration(refreshToken).toInstant();

        // Detect replay before doing anything!
        Optional<InvalidatedToken> maybeExisting = invalidatedTokenRepositoryPort.findByJti(jti);
        if (maybeExisting.isPresent()) {
            throw new TokenException("Refresh token replay detected", userId);
        }

        // Persist old JTI as invalidated using the token's expiry
        invalidatedTokenRepositoryPort.save(new InvalidatedToken(jti, userId, expiry));

        // Generate new tokens (fresh access and refresh) in a pair
        String newAccessToken = tokenProviderPort.generateAccessToken(username, userId, metadata);
        String newRefreshToken = tokenProviderPort.generateRefreshToken(username, userId, metadata);

        return new TokenPair(newAccessToken, newRefreshToken);
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
    }

    @Override
    @Transactional
    public void invalidateAllTokensForUser(Long userId) {
        invalidatedTokenRepositoryPort.deleteByUserId(userId);
    }
}
