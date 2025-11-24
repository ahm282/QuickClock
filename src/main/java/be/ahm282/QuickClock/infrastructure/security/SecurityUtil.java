package be.ahm282.QuickClock.infrastructure.security;

import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    private final TokenProviderPort tokenProviderPort;

    public SecurityUtil(TokenProviderPort tokenProviderPort) {
        this.tokenProviderPort = tokenProviderPort;
    }

    public Authentication getAuthenticationOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return auth;
    }

    public boolean hasAdminRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_SUPER_ADMIN".equals(a));
    }

    public void requireAdmin() {
        Authentication auth = getAuthenticationOrThrow();
        if (!hasAdminRole(auth)) {
            throw new AccessDeniedException("Admin privileges are required for this operation.");
        }
    }

    public Long extractUserIdFromRequestToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("Not authenticated");
        }

        String token = header.substring(7);
        return tokenProviderPort.extractUserId(token);
    }
}
