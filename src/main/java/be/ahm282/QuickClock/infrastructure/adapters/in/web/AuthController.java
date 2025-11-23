package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.AuthUseCase;
import be.ahm282.QuickClock.application.ports.in.RefreshTokenUseCase;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.domain.exception.TokenException;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.AccessTokenResponseDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.ErrorResponseDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.LoginRequestDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.RegisterRequestDTO;
import be.ahm282.QuickClock.infrastructure.security.service.RateLimitService;
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
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int REFRESH_COOKIE_MAX_AGE = 2_592_000; // 30 days

    private final AuthUseCase authUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final TokenProviderPort tokenProviderPort;
    private final RateLimitService rateLimitService;

    @org.springframework.beans.factory.annotation.Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @org.springframework.beans.factory.annotation.Value("${app.cookie.domain:}")
    private String cookieDomain;

    public AuthController(AuthUseCase authUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          TokenProviderPort tokenProviderPort,
                          RateLimitService rateLimitService) {
        this.authUseCase = authUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.tokenProviderPort = tokenProviderPort;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String rateLimitKey = ipAddress;

        if (!rateLimitService.allowRegisterAttempt(rateLimitKey)) {
            log.warn("Rate limit exceeded for registration attempt from: {}", rateLimitKey);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponseDTO("Too many registration attempts. Please try again later.", 429));
        }

        try {
            Long userId = authUseCase.register(request.username(), request.password());
            return ResponseEntity.status(HttpStatus.CREATED).body(userId);
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("Username already exists", 409)
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String ipAddress = getClientIp(httpRequest);
        String rateLimitKey = ipAddress + ":" + request.username();

        if (!rateLimitService.allowLoginAttempt(rateLimitKey)) {
            log.warn("Rate limit exceeded for login attempt: {}", rateLimitKey);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponseDTO("Too many login attempts. Please try again later.", 429));
        }

        try {
            TokenPair pair = authUseCase.login(request.username(), request.password());

            // Reset rate limit on successful login
            rateLimitService.resetLoginLimit(rateLimitKey);
            addRefreshTokenCookie(response, pair.refreshToken());

            return ResponseEntity.ok(new AccessTokenResponseDTO(pair.accessToken()));
        } catch (IllegalArgumentException e) {
            // Failed attempt already consumed a token from the bucket
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDTO("Authentication failed. Please log in again.", 401));
        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Internal server error. Authentication failed. Please log in again.", 500));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String ipAddress = getClientIp(request);
        String rateLimitKey = "refresh:" + ipAddress;

        // Check rate limit
        if (!rateLimitService.allowRefreshAttempt(rateLimitKey)) {
            log.warn("Rate limit exceeded for refresh attempt: {}", rateLimitKey);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponseDTO("Too many refresh attempts. Please try again later.", 429));
        }

        try {
            String refreshToken = extractRefreshTokenFromCookies(request);

            if (refreshToken == null || !tokenProviderPort.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponseDTO("Invalid refresh token", 401));
            }

            TokenPair pair = refreshTokenUseCase.rotateRefreshTokenByToken(refreshToken);

            // Reset rate limit on successful refresh
            rateLimitService.resetRefreshLimit(rateLimitKey);

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
            String accessToken = extractAccessTokenFromHeader(request);
            refreshTokenUseCase.logout(accessToken, refreshToken);
        } finally {
            clearRefreshTokenCookie(response);
        }
        return ResponseEntity.ok().build();
    }

    // --- Cookie Helpers ---
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);

        // Only set domain if configured (empty string means don't set domain)
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }

        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(REFRESH_COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);

        // Only set domain if configured
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }

        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setValue(null);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private String extractAccessTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
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

    private String getClientIp(HttpServletRequest request) { // TODO Re-evaluate this method if behind a proxy
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
