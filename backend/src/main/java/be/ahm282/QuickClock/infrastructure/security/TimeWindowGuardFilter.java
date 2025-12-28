package be.ahm282.QuickClock.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class TimeWindowGuardFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TimeWindowGuardFilter.class);

    // Configuration constants
    private static final String HEADER_NAME = "X-QuickClock-Guard";
    private static final String PREFIX = "TeaTime";
    private static final String SECRET_SALT = "QuickClock_Salt_v1"; // Must match Frontend
    private static final long MAX_AGE_SECONDS = 60; // 60 seconds validity window

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Skip check for CORS Pre-flight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerValue = request.getHeader(HEADER_NAME);

        if (headerValue == null) {
            blockRequest(response, "Missing guard header");
            return;
        }

        try {
            // 2. Decode Base64
            byte[] decodedBytes = Base64.getDecoder().decode(headerValue);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

            // 3. Split parts -> Expected: TeaTime:1703698235000:QuickClock_Salt_v1
            String[] parts = decodedString.split(":");

            if (parts.length != 3) {
                blockRequest(response, "Invalid header format");
                return;
            }

            String prefix = parts[0];
            long timestamp = Long.parseLong(parts[1]);
            String salt = parts[2];

            // 4. Verify Content
            if (!PREFIX.equals(prefix) || !SECRET_SALT.equals(salt)) {
                blockRequest(response, "Invalid prefix or salt");
                return;
            }

            // 5. Verify Time Window (Prevents Replay Attacks)
            long now = Instant.now().toEpochMilli();
            long diffSeconds = (now - timestamp) / 1000;

            // Allow if generated within last 60s, or up to 5s in the future (clock drift)
            if (diffSeconds > MAX_AGE_SECONDS || diffSeconds < -5) {
                blockRequest(response, "Request expired (Timestamp: " + timestamp + ")");
                return;
            }

        } catch (IllegalArgumentException e) {
            blockRequest(response, "Base64 decoding error");
            return;
        }

        // 6. Proceed if valid
        filterChain.doFilter(request, response);
    }

    private void blockRequest(HttpServletResponse response, String reason) throws IOException {
        log.warn("Bot/Scanner Blocked: {}", reason);
        response.setStatus(HttpStatus.I_AM_A_TEAPOT.value()); // 418
        response.setContentType("text/plain");
        response.getWriter().write("I am a teapot. " + reason);
        response.getWriter().flush();
    }
}