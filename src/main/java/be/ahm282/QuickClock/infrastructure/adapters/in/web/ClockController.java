package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeRequestDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeResponseDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockRequestDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockResponseDTO;
import be.ahm282.QuickClock.application.ports.out.TokenProviderPort;
import be.ahm282.QuickClock.application.services.ClockService;
import be.ahm282.QuickClock.application.services.QRCodeService;
import be.ahm282.QuickClock.domain.exception.RateLimitException;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.AdminClockRequestDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.mapper.ClockResponseDTOMapper;
import be.ahm282.QuickClock.infrastructure.security.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clock")
public class ClockController {

    private final ClockService clockService;
    private final QRCodeService qrCodeService;
    private final ClockResponseDTOMapper responseMapper;
    private final RateLimitService rateLimitService;
    private final TokenProviderPort tokenProviderPort;

    public ClockController(ClockService clockService,
                           QRCodeService qrCodeService,
                           ClockResponseDTOMapper responseMapper,
                           RateLimitService rateLimitService,
                           TokenProviderPort tokenProviderPort) {
        this.clockService = clockService;
        this.qrCodeService = qrCodeService;
        this.responseMapper = responseMapper;
        this.rateLimitService = rateLimitService;
        this.tokenProviderPort = tokenProviderPort;
    }

    // -------------------------------------------------------------------------
    // TEMP ID-based endpoints (dev/testing only)
    // -------------------------------------------------------------------------

    @PostMapping("/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockIn(@RequestBody @Valid ClockRequestDTO requestDTO) {
        // Consider restricting this to admins only, or removing once QR flow is stable
        ClockRecord record = clockService.clockIn(requestDTO.getUserId());
        return responseMapper.toDTO(record);
    }

    @PostMapping("/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockOut(@RequestBody @Valid ClockRequestDTO requestDTO) {
        // Consider restricting this to admins only, or removing once QR flow is stable
        ClockRecord record = clockService.clockOut(requestDTO.getUserId());
        return responseMapper.toDTO(record);
    }

    // -------------------------------------------------------------------------
    // History endpoints
    // - Admin / Super Admin: can read any user's history
    // - Others: only their own
    // -------------------------------------------------------------------------
    @GetMapping("/history/{userId}")
    public List<ClockResponseDTO> getHistory(@PathVariable Long userId,
                                             HttpServletRequest request) {
        Authentication auth = getAuthenticationOrThrow();

        boolean isAdmin = hasAdminRole(auth);
        if (!isAdmin) {
            Long tokenUserId = extractUserIdFromRequestToken(request);
            if (!userId.equals(tokenUserId)) {
                throw new AccessDeniedException("You are not allowed to view another user's history.");
            }
        }

        return clockService.getHistory(userId)
                .stream()
                .map(responseMapper::toDTO)
                .toList();
    }

    @GetMapping("/history/me")
    public List<ClockResponseDTO> getMyHistory(HttpServletRequest request) {
        Long userId = extractUserIdFromRequestToken(request);
        return getHistory(userId, request);
    }

    // -------------------------------------------------------------------------
    // QR-based endpoints (employee flow, initiated from kiosk)
    // -------------------------------------------------------------------------

    @GetMapping("/qr/generate/in/{userId}")
    public ClockQRCodeResponseDTO generateClockInQRCode(@PathVariable Long userId) {
        // This is kiosk/admin-side functionality → protect it
        requireAdmin();
        return qrCodeService.generateClockInQRCode(userId);
    }

    @GetMapping("/qr/generate/out/{userId}")
    public ClockQRCodeResponseDTO generateClockOutQRCode(@PathVariable Long userId) {
        // This is kiosk/admin-side functionality → protect it
        requireAdmin();
        return qrCodeService.generateClockOutQRCode(userId);
    }

    @PostMapping("/qr/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockInWithQR(@RequestBody @Valid ClockQRCodeRequestDTO request,
                                          HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        if (!rateLimitService.allowClockQrAttempt(ipAddress)) {
            throw new RateLimitException("Too many QR code clock-in attempts. Please try again later.");
        }

        // TODO (important, next step): cross-check that QR token's userId == JWT userId
        ClockRecord record = clockService.clockInWithQR(request.getToken());
        return responseMapper.toDTO(record);
    }

    @PostMapping("/qr/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockOutWithQR(@RequestBody @Valid ClockQRCodeRequestDTO request,
                                           HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        if (!rateLimitService.allowClockQrAttempt(ipAddress)) {
            throw new RateLimitException("Too many QR code clock-out attempts. Please try again later.");
        }

        // TODO (important, next step): cross-check that QR token's userId == JWT userId
        ClockRecord record = clockService.clockOutWithQR(request.getToken());
        return responseMapper.toDTO(record);
    }

    // -------------------------------------------------------------------------
// Admin manual clocking: timestamp + reason
// -------------------------------------------------------------------------

    @PostMapping("/admin/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO adminClockIn(@RequestBody @Valid AdminClockRequestDTO request) {
        requireAdmin();
        ClockRecord record = clockService.adminClockIn(
                request.userId(),
                request.timestamp(),
                request.reason()
        );
        return responseMapper.toDTO(record);
    }

    @PostMapping("/admin/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO adminClockOut(@RequestBody @Valid AdminClockRequestDTO request) {
        requireAdmin();
        ClockRecord record = clockService.adminClockOut(
                request.userId(),
                request.timestamp(),
                request.reason()
        );
        return responseMapper.toDTO(record);
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private Authentication getAuthenticationOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return auth;
    }

    private boolean hasAdminRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_SUPER_ADMIN".equals(a));
    }

    private void requireAdmin() {
        Authentication auth = getAuthenticationOrThrow();
        if (!hasAdminRole(auth)) {
            throw new AccessDeniedException("Admin privileges are required for this operation.");
        }
    }

    private Long extractUserIdFromRequestToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("Not authenticated");
        }
        String token = header.substring(7);
        return tokenProviderPort.extractUserId(token);
    }

    private String getClientIp(HttpServletRequest request) { // TODO Re-evaluate this method if behind a proxy
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
