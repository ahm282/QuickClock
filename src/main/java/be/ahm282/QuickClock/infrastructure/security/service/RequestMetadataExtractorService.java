package be.ahm282.QuickClock.infrastructure.security.service;

import be.ahm282.QuickClock.application.dto.TokenMetadata;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RequestMetadataExtractorService {
    private static final String DEVICE_ID_COOKIE = "deviceId";
    private static final int DEVICE_ID_MAX_AGE = 31_536_000; // 1 year

    public TokenMetadata extract(HttpServletRequest request) {
        String deviceId = extractOrCreateDeviceId(request);
        String ipAddress = getClientIp(request);
        String userAgent = extractUserAgent(request);

        return new TokenMetadata(deviceId, ipAddress, userAgent);
    }

    private String extractOrCreateDeviceId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (DEVICE_ID_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return UUID.randomUUID().toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0];
            }
        }

        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        // Truncate if too long to not bloat JWT
        return userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent;
    }

    public Cookie createDeviceIdCookie(String deviceId, boolean secure) {
        Cookie cookie = new Cookie(DEVICE_ID_COOKIE, deviceId);
        cookie.setMaxAge(DEVICE_ID_MAX_AGE);
        cookie.setHttpOnly(false);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(DEVICE_ID_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}
