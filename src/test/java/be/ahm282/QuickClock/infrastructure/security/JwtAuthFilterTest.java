package be.ahm282.QuickClock.infrastructure.security;

import be.ahm282.QuickClock.application.ports.out.InvalidatedTokenRepositoryPort;
import be.ahm282.QuickClock.infrastructure.security.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtAuthFilterTest {

    private JwtTokenService jwtTokenService;
    private InvalidatedTokenRepositoryPort invalidatedTokenRepository;
    private JwtAuthFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setup() throws Exception {
        jwtTokenService = mock(JwtTokenService.class);
        invalidatedTokenRepository = mock(InvalidatedTokenRepositoryPort.class);
        filter = new JwtAuthFilter(jwtTokenService, invalidatedTokenRepository);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        SecurityContextHolder.clearContext();

        // By default, no tokens are invalidated
        when(invalidatedTokenRepository.findByJti(any())).thenReturn(Optional.empty());
    }

    // ---------------------------------------------------------
    // No Authorization header → filter passes through untouched
    // ---------------------------------------------------------
    @Test
    void shouldSkipWhenNoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ---------------------------------------------------------
    // Invalid token → 401
    // ---------------------------------------------------------
    @Test
    void shouldReturn401WhenJwtExceptionThrown() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        when(jwtTokenService.parseClaims("invalid")).thenThrow(new JwtException("invalid"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(response.getWriter()).write(anyString());
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ---------------------------------------------------------
    // Valid access token → authenticate user
    // ---------------------------------------------------------
    @Test
    void shouldAuthenticateWhenValidAccessToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");

        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("jti-123");
        when(claims.getSubject()).thenReturn("john");
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.get("roles", List.class)).thenReturn(List.of("USER"));

        when(jwtTokenService.parseClaims("valid")).thenReturn(claims);


        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("john", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        verify(filterChain).doFilter(request, response);
    }

    // ---------------------------------------------------------
    // Non-access tokens (refresh) → rejected with 401
    // ---------------------------------------------------------
    @Test
    void shouldRejectNonAccessToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");

        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("refresh");

        when(jwtTokenService.parseClaims("valid")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
