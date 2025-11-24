package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.dto.TokenPair;
import be.ahm282.QuickClock.application.ports.in.AuthUseCase;
import be.ahm282.QuickClock.application.ports.in.RefreshTokenUseCase;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.domain.exception.AuthenticationException;
import be.ahm282.QuickClock.domain.exception.RateLimitException;
import be.ahm282.QuickClock.domain.exception.TokenException;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.AccessTokenResponseDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.LoginRequestDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.RegisterRequestDTO;
import be.ahm282.QuickClock.infrastructure.security.service.RateLimitService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequestDTO request, HttpServletRequest httpRequest) {
        String rateLimitKey = getClientIp(httpRequest);

        if (!rateLimitService.allowRegisterAttempt(rateLimitKey)) {
            log.warn("Rate limit exceeded for registration attempt from: {}", rateLimitKey);
            throw new RateLimitException("Too many registration attempts. Please try again later.");
        }

        authUseCase.register(request.username(), request.password(), request.inviteCode());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponseDTO> login(@RequestBody @Valid LoginRequestDTO request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String ipAddress = getClientIp(httpRequest);
        String rateLimitKey = ipAddress + ":" + request.username();

        if (!rateLimitService.allowLoginAttempt(rateLimitKey)) {
            log.warn("Rate limit exceeded for login attempt: {}", rateLimitKey);
            throw new RateLimitException("Too many login attempts. Please try again later.");
        }

        TokenPair pair = authUseCase.login(request.username(), request.password());

        // Reset rate limit on successful login
        rateLimitService.resetLoginLimit(rateLimitKey);
        addRefreshTokenCookie(response, pair.refreshToken());

        return ResponseEntity.ok(new AccessTokenResponseDTO(pair.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponseDTO> refresh(HttpServletRequest request, HttpServletResponse response) {
        String ipAddress = getClientIp(request);
        String rateLimitKey = "refresh:" + ipAddress;

        // Check rate limit
        if (!rateLimitService.allowRefreshAttempt(rateLimitKey)) {
            log.warn("Rate limit exceeded for refresh attempt: {}", rateLimitKey);
            throw new RateLimitException("Too many refresh attempts. Please try again later.");
        }

        String refreshToken = extractRefreshTokenFromCookies(request);

        if (refreshToken == null || !tokenProviderPort.isRefreshToken(refreshToken)) {
            clearRefreshTokenCookie(response);
            throw new AuthenticationException("Invalid refresh token");
        }

        try {
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
            throw e;
        } catch (JwtException e) {
            log.warn("Invalid JWT on refresh {}", e.getMessage());
            clearRefreshTokenCookie(response);
            throw e;
        } catch (Exception e) {
            log.error("Error during token refresh", e);
            clearRefreshTokenCookie(response);
            throw e;
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


    private String getClientIp(HttpServletRequest request) { // TODO Re-evaluate this method if behind a proxy
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
