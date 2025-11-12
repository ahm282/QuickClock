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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponseDTO("Invalid credentials", 401));
            }

            String accessToken = jwtTokenService.generateAccessToken(user);
            String refreshToken = jwtTokenService.generateRefreshToken(user);

            // Store refresh token in HTTP-only cookie
            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
            refreshTokenCookie.setHttpOnly(true);  // Cannot be accessed by JavaScript
            refreshTokenCookie.setSecure(true);     // Only sent over HTTPS
            refreshTokenCookie.setPath("/api/auth/refresh");  // Only sent to refresh endpoint
            refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60);  // 30 days
            refreshTokenCookie.setAttribute("SameSite", "Strict");  // CSRF protection
            response.addCookie(refreshTokenCookie);

            return ResponseEntity.ok(new AccessTokenResponseDTO(accessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDTO("Invalid credentials", 401));
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

            String newAccessToken = jwtTokenService.generateAccessToken(user);
            String newRefreshToken = jwtTokenService.generateRefreshToken(user);

            // Store refresh token in HTTP-only cookie
            Cookie refreshTokenCookie = new Cookie("refreshToken", newRefreshToken);
            refreshTokenCookie.setHttpOnly(true);  // Cannot be accessed by JavaScript
            refreshTokenCookie.setSecure(false);     // Enable in Production
            refreshTokenCookie.setPath("/api/auth/refresh");  // Only sent to refresh endpoint
            refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60);  // 30 days
            refreshTokenCookie.setAttribute("SameSite", "Strict");  // CSRF protection
            response.addCookie(refreshTokenCookie);

            return ResponseEntity.ok(new AccessTokenResponseDTO(newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDTO("Invalid or expired refresh token", 401));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {  // <-- Add this parameter
        // Clear refresh token cookie
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);  // Set to false for local development
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(0);  // Delete cookie immediately
        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }
}
