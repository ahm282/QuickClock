package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.application.dto.TokenPairDTO;
import be.ahm282.QuickClock.domain.exception.TokenException;


public interface RefreshTokenUseCase {
    /**
     * Atomically rotate a refresh token: validate the provided refresh token,
     * guard against replay (jti blacklist), invalidate the old jti and return a new TokenPair.
     *
     * @param refreshToken Full JWT string.
     * @return new TokenPair(access, refresh)
     * @throws TokenException when replay or other token-level security problem is detected.
     */
    TokenPairDTO rotateRefreshTokenByToken(String refreshToken) throws TokenException;

    /**
     * Invalidate the refresh token by its JWT string (for logout).
     * Will store the JTI into the invalidated table using the token's expiration time.
     *
     * @param refreshToken The refresh token to invalidate.
     */
    void invalidateRefreshToken(String refreshToken);

    /**
     * Invalidates all tokens for a given user id.
     * @param userId the ID of the user.
     */
    void invalidateAllTokensForUser(Long userId);

    /**
     * Logout the current session: invalidate the given refresh token and
     * blacklist the current access token so it cannot be reused.
     *
     * @param refreshToken refresh token from cookie (maybe null)
     * @param accessToken  access token from Authorization header (maybe null)
     */
    void logout(String refreshToken, String accessToken);
}


