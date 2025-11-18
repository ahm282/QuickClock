package be.ahm282.QuickClock.infrastructure.security;

import be.ahm282.QuickClock.application.dto.TokenMetadata;
import be.ahm282.QuickClock.infrastructure.security.service.JwtTokenService;
import be.ahm282.QuickClock.infrastructure.security.service.RequestMetadataExtractorService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtAuthFilterTest {

    private JwtTokenService jwtTokenService;
    private RequestMetadataExtractorService metadataExtractor;
    private JwtAuthFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setup() throws Exception {
        jwtTokenService = mock(JwtTokenService.class);
        metadataExtractor = mock(RequestMetadataExtractorService.class);
        filter = new JwtAuthFilter(jwtTokenService, metadataExtractor);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        SecurityContextHolder.clearContext();
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
        when(claims.getSubject()).thenReturn("john");
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.get("roles", List.class)).thenReturn(List.of("USER"));
        when(claims.get("deviceId", String.class)).thenReturn("abc");
        when(claims.get("ipAddress", String.class)).thenReturn("1.2.3.4");

        when(jwtTokenService.parseClaims("valid")).thenReturn(claims);

        when(metadataExtractor.extract(request)).thenReturn(new TokenMetadata("abc", "1.2.3.4", "PostmanRuntime/7.32.0"));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("john", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        verify(filterChain).doFilter(request, response);
    }

    // ---------------------------------------------------------
    // Non-access tokens (refresh) → ignored
    // ---------------------------------------------------------
    @Test
    void shouldIgnoreNonAccessToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");

        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("refresh");

        when(jwtTokenService.parseClaims("valid")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ---------------------------------------------------------
    // Device/IP mismatch → still authenticates but logs
    // ---------------------------------------------------------
    @Test
    void shouldLogMetadataMismatchButStillAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("alice");
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.get("roles", List.class)).thenReturn(List.of("ADMIN"));

        when(claims.get("deviceId", String.class)).thenReturn("token-device");
        when(claims.get("ipAddress", String.class)).thenReturn("9.9.9.9");

        when(jwtTokenService.parseClaims("valid")).thenReturn(claims);

        when(metadataExtractor.extract(request))
                .thenReturn(new TokenMetadata("other-device", "3.3.3.3", "PostmanRuntime/7.32.0"));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
