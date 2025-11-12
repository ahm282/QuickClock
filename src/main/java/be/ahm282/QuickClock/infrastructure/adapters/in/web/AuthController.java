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
record JwtResponseDTO(String token) {}

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
    public ResponseEntity<Void> register(@RequestBody RegisterRequestDTO request) {
        userService.createUser(request.username(), passwordEncoder.encode(request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@RequestBody LoginRequestDTO request) {
        UserEntity user = userService.findByUsername(request.username());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtTokenService.generateToken(user);
        return ResponseEntity.ok(new JwtResponseDTO(token));
    }
}
