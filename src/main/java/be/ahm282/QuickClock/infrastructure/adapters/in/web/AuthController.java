package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.AuthUseCase;
import be.ahm282.QuickClock.application.ports.in.RefreshTokenUseCase;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.domain.exception.TokenException;
import be.ahm282.QuickClock.domain.model.User;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.AccessTokenResponseDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.ErrorResponseDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.LoginRequestDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.RegisterRequestDTO;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper.UserMapper;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final static Logger log = LoggerFactory.getLogger(AuthController.class);
    private final static int REFRESH_TOKEN_MAX_AGE = 30 * 24 * 60 * 60; // 30 days

    private final AuthUseCase authUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final TokenProviderPort tokenProviderPort;

    public AuthController(AuthUseCase authUseCase, RefreshTokenUseCase refreshTokenUseCase, TokenProviderPort tokenProviderPort) {
        this.authUseCase = authUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.tokenProviderPort = tokenProviderPort;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO request) {
        try {
            Long userId = authUseCase.register(request.username(),request.password());
            return ResponseEntity.status(HttpStatus.CREATED).body(userId);
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("Username already exists", 409)
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request, HttpServletResponse response) {
        try {
            TokenPair pair = authUseCase.login(request.username(), request.password());
            addRefreshTokenCookie(response, pair.refreshToken());
            return ResponseEntity.ok(new AccessTokenResponseDTO(pair.accessToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDTO("Invalid credentials", 401));
        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Internal server error", 500));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = extractRefreshTokenFromCookies(request);

            if (refreshToken == null || !tokenProviderPort.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponseDTO("Invalid refresh token", 401));
            }

            TokenPair pair = refreshTokenUseCase.rotateRefreshTokenByToken(refreshToken);
            addRefreshTokenCookie(response, pair.refreshToken());

            return ResponseEntity.ok(new AccessTokenResponseDTO(pair.accessToken()));
        } catch (TokenException e) {
            log.warn("!!! SECURITY ALERT: Token replay detected. Invalidating all sessions for user ID: {}", e.getUserId());
            if (e.getUserId() != null) {
                refreshTokenUseCase.invalidateAllTokensForUser(e.getUserId());
            }
            clearRefreshTokenCookie(response);
            return unauthorized(e.getMessage());
        } catch (JwtException e) {
            log.warn("Invalid JWT on refresh {}", e.getMessage());
            clearRefreshTokenCookie(response);
            return unauthorized("Invalid or expired refresh token");
        } catch (Exception e) {
            log.error("Error during token refresh", e);
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Server error", 500));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = extractRefreshTokenFromCookies(request);
            if (refreshToken != null) {
                refreshTokenUseCase.invalidateRefreshToken(refreshToken);
            }
        } catch (JwtException e) {
            log.warn("Invalid JWT during logout: {}", e.getMessage());
        } finally {
            clearRefreshTokenCookie(response);
        }

        return ResponseEntity.ok().build();
    }

    // --- Cookie Helpers ---
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set to false for local dev
        cookie.setPath("/");
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setValue(null);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private ResponseEntity<ErrorResponseDTO> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO(message, 401));
    }
}
