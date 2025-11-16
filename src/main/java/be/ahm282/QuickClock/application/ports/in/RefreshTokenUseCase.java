package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.domain.exception.TokenException;
import be.ahm282.QuickClock.domain.model.User;


public interface RefreshTokenUseCase {
    /**
     * Verifies a refresh token (jti), invalidates it, then generates a new one.
     *
     * @param oldTokenJti The JTI (ID) of the refresh token being used.
     * @param user The user for whom the token is being refreshed.
     * @return A new, valid refresh token (JWT string)
     * @throws TokenException if the token JTI is invalid, already used or blacklisted.
     */
    String rotateRefreshToken(String oldTokenJti, User user);

    /**
     * Invalidates a specific refresh token JTI, typically for logout.
     *
     * @param jti The JTI (ID) of the refresh token to invalidate.
     */
    void invalidateToken(String jti);

    /**
     * Invalidates all active refresh tokens for a specific user.
     * Crucial step for "sign out everywhere" or detected attacks.
      * @param userId the ID of the user.
     */
    void invalidateAllTokensForUser(Long userId);
}


