package be.ahm282.QuickClock.infrastructure.security;

import be.ahm282.QuickClock.application.ports.out.InvalidatedTokenRepositoryPort;
import be.ahm282.QuickClock.infrastructure.security.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.*;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final InvalidatedTokenRepositoryPort invalidatedTokenRepository;

    public JwtAuthFilter(JwtTokenService jwtTokenService,
                        InvalidatedTokenRepositoryPort invalidatedTokenRepository) {
        this.jwtTokenService = jwtTokenService;
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtTokenService.parseClaims(token);

            if (!isAccessToken(claims)) {
                log.warn("Non-access token used in Authorization header");
                sendError(response);
                return;
            }

            // Check if the token is blacklisted/invalidated
            String jti = claims.getId();
            boolean isTokenBlacklistedOrInvalidated = jti != null && invalidatedTokenRepository.findByJti(jti).isPresent();

            if (isTokenBlacklistedOrInvalidated) {
                log.warn("Invalidated token attempted use: jti={}", jti);
                sendError(response);
                return;
            }

            authenticate(claims, request);
        }  catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            sendError(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ------------------------
    // Extraction and Validation
    // ------------------------
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    private boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }


    // ------------------------
    // Authentication
    // ------------------------
    private void authenticate(Claims claims, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return; // Already authenticated
        }

        String username = claims.getSubject();
        if (username == null) {
            return;
        }

        List<?> rawRoles = claims.get("roles", List.class);
        List<String> roles = rawRoles == null
                ? Collections.emptyList()
                : rawRoles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();

        List<GrantedAuthority> authorities =
                roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(toUnmodifiableList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                authorities
        );

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ------------------------
    // Error Handling
    // ------------------------
    private void sendError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        String body = """
        {
          "recordedAt": "%s",
          "status": 401,
          "error": "Unauthorized",
          "message": "Authentication failed. Please try again.",
          "type": "AuthenticationFailure"
        }
        """.formatted(Instant.now().toString());
        response.getWriter().write(body);
    }
}
