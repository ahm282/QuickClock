package be.ahm282.QuickClock.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeWindowGuardFilterTest {

    private TimeWindowGuardFilter filter;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new TimeWindowGuardFilter();
        // REMOVED: when(response.getWriter())... because it causes unnecessary stubbing errors on success tests
    }

    @Test
    void shouldAllow_ValidHeader() throws Exception {
        // Given
        long now = Instant.now().toEpochMilli();
        String validPayload = "TeaTime:" + now + ":QuickClock_Salt_v1";
        String headerValue = Base64.getEncoder().encodeToString(validPayload.getBytes(StandardCharsets.UTF_8));

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-QuickClock-Guard")).thenReturn(headerValue);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void shouldSkip_OptionsMethod() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(request, never()).getHeader(anyString());
    }

    @Test
    void shouldBlock_ExpiredTimestamp() throws Exception {
        // Setup Writer for this specific test
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // Given: Time 61 seconds ago
        long past = Instant.now().minusSeconds(61).toEpochMilli();
        String payload = "TeaTime:" + past + ":QuickClock_Salt_v1";
        String headerValue = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-QuickClock-Guard")).thenReturn(headerValue);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpStatus.I_AM_A_TEAPOT.value());
    }

    @Test
    void shouldBlock_WrongSalt() throws Exception {
        // Setup Writer for this specific test
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        long now = Instant.now().toEpochMilli();
        String payload = "TeaTime:" + now + ":WRONG_SALT";
        String headerValue = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-QuickClock-Guard")).thenReturn(headerValue);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpStatus.I_AM_A_TEAPOT.value());
    }

    @Test
    void shouldBlock_MissingHeader() throws Exception {
        // Setup Writer for this specific test
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-QuickClock-Guard")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpStatus.I_AM_A_TEAPOT.value());
    }
}