package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.UserService;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import be.ahm282.QuickClock.infrastructure.security.JwtTokenService;
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
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        try {
            UserEntity user = userService.findByUsername(request.username());

            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponseDTO("Invalid credentials", 401));
            }

            String accessToken = jwtTokenService.generateAccessToken(user);
            String refreshToken = jwtTokenService.generateRefreshToken(user);

            return ResponseEntity.ok(new TokenResponseDTO(accessToken, refreshToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDTO("Invalid credentials", 401));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequestDTO request) {
        try {
            if (!jwtTokenService.isRefreshToken(request.refreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponseDTO("Invalid refresh token", 401));
            }

            String username = jwtTokenService.extractUsername(request.refreshToken());
            UserEntity user = userService.findByUsername(username);

            String newAccessToken = jwtTokenService.generateAccessToken(user);
            String newRefreshToken = jwtTokenService.generateRefreshToken(user);

            return ResponseEntity.ok(new TokenResponseDTO(newAccessToken, newRefreshToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDTO("Invalid or expired refresh token", 401));
        }
    }
}
