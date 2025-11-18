package be.ahm282.QuickClock.infrastructure.security;

import be.ahm282.QuickClock.application.dto.TokenMetadata;
import be.ahm282.QuickClock.infrastructure.security.service.JwtTokenService;
import be.ahm282.QuickClock.infrastructure.security.service.RequestMetadataExtractorService;
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
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.*;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final RequestMetadataExtractorService metadataExtractor;

    public JwtAuthFilter(JwtTokenService jwtTokenService, RequestMetadataExtractorService metadataExtractor) {
        this.jwtTokenService = jwtTokenService;
        this.metadataExtractor = metadataExtractor;
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
                filterChain.doFilter(request, response); // Refresh token or another type, ignore.
                return;
            }

            validateMetadata(claims, request);
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

    private void validateMetadata(Claims claims, HttpServletRequest request) {
        TokenMetadata currentMetadata = metadataExtractor.extract(request);

        String tokenDeviceId = claims.get("deviceId", String.class);
        String tokenIp = claims.get("ipAddress", String.class);

        if (tokenDeviceId != null && !tokenDeviceId.equals(currentMetadata.deviceId())) {
            log.warn("Device ID mismatch for user {}. Token stolen?", claims.getSubject());
        }

        if (tokenIp != null && !tokenIp.equals(currentMetadata.ipAddress())) {
            log.info("IP change detected for user {}", claims.getSubject());
        }
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
        response.getWriter().write(String.format("{\"error\": \"%s\", \"status\": 401}", "Authentication failed. Please log in again."));
    }
}
