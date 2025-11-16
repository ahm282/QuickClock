package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.UserService;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import be.ahm282.QuickClock.infrastructure.security.JwtTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.password.PasswordEncoder;

record LoginRequestDTO(String username, String password) {}
record RegisterRequestDTO(String username, String password) {}
record RefreshRequestDTO(String refreshToken) {}
record TokenResponseDTO(String accessToken, String refreshToken) {}
record AccessTokenResponseDTO(String accessToken) {}
record ErrorResponseDTO(String errorMessage, int status) {}

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final static int REFRESH_TOKEN_MAX_AGE = 30 * 24 * 60 * 60; // 30 days

    public AuthController(UserService userService, JwtTokenService jwtTokenService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO request) {
        try {
            userService.createUser(request.username(), passwordEncoder.encode(request.password()));
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponseDTO("Username already exists", 409));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request, HttpServletResponse response) {
        try {
            UserEntity user = userService.findByUsername(request.username());

            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                return unauthorized("Invalid credentials");
            }

            String accessToken = jwtTokenService.generateAccessToken(user.getUsername(), user.getId());
            String refreshToken = jwtTokenService.generateRefreshToken(user.getUsername(), user.getId());

            // Store refresh token in HTTP-only cookie
            addRefreshTokenCookie(response, refreshToken);

            return ResponseEntity.ok(new AccessTokenResponseDTO(accessToken));
        } catch (Exception e) {
            return unauthorized("Invalid credentials");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
           String refreshToken = null;
           if (request.getCookies() != null) {
               for (Cookie cookie : request.getCookies()) {
                   if ("refreshToken".equals(cookie.getName())) {
                       refreshToken = cookie.getValue();
                       break;
                   }
               }
           }

           if (refreshToken == null || !jwtTokenService.isRefreshToken(refreshToken)) {
               return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                       .body(new ErrorResponseDTO("Invalid refresh token", 401));
           }

            String username = jwtTokenService.extractUsername(refreshToken);
            UserEntity user = userService.findByUsername(username);

            String newAccessToken = jwtTokenService.generateAccessToken(user.getUsername(), user.getId());
            String newRefreshToken = jwtTokenService.generateRefreshToken(user.getUsername(), user.getId());

            // Store refresh token in HTTP-only cookie
            addRefreshTokenCookie(response, newRefreshToken);

            return ResponseEntity.ok(new AccessTokenResponseDTO(newAccessToken));
        } catch (Exception e) {
            return unauthorized("Invalid or expired refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {  // <-- Add this parameter
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    // --- Helpers ---
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set to false for local dev
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
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
