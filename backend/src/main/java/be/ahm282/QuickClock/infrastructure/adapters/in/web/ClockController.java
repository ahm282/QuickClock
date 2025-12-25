package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.dto.WorkHoursDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeRequestDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeResponseDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockResponseDTO;
import be.ahm282.QuickClock.application.services.ClockService;
import be.ahm282.QuickClock.application.services.QRCodeService;
import be.ahm282.QuickClock.application.services.WorkHoursService;
import be.ahm282.QuickClock.domain.exception.RateLimitException;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.AdminClockRequestDTO;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.mapper.ClockResponseDTOMapper;
import be.ahm282.QuickClock.infrastructure.security.SecurityUtil;
import be.ahm282.QuickClock.infrastructure.security.service.RateLimitService;
import be.ahm282.QuickClock.infrastructure.sse.QrScanPushService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@RestController
@RequestMapping("/api/clock")
public class ClockController {

    private final ClockService clockService;
    private final QRCodeService qrCodeService;
    private final ClockResponseDTOMapper responseMapper;
    private final RateLimitService rateLimitService;
    private final SecurityUtil securityUtil;
    private final QrScanPushService qrScanPushService;
    private final WorkHoursService workHoursService;

    public ClockController(ClockService clockService,
                           QRCodeService qrCodeService,
                           ClockResponseDTOMapper responseMapper,
                           RateLimitService rateLimitService,
                           SecurityUtil securityUtil,
                           QrScanPushService qrScanPushService,
                           WorkHoursService workHoursService) {
        this.clockService = clockService;
        this.qrCodeService = qrCodeService;
        this.responseMapper = responseMapper;
        this.rateLimitService = rateLimitService;
        this.securityUtil = securityUtil;
        this.qrScanPushService = qrScanPushService;
        this.workHoursService = workHoursService;
    }

    // -------------------------------------------------------------------------
    // TEMP ID-based endpoints (dev/testing only)
    // -------------------------------------------------------------------------

    @PostMapping("/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockIn(HttpServletRequest request) {
        Long userId = securityUtil.extractUserIdFromRequestToken(request);
        ClockRecord record = clockService.clockIn(userId);
        return responseMapper.toDTO(record);
    }

    @PostMapping("/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockOut(HttpServletRequest request) {
        Long userId = securityUtil.extractUserIdFromRequestToken(request);
        ClockRecord record = clockService.clockOut(userId);
        return responseMapper.toDTO(record);
    }

    // -------------------------------------------------------------------------
    // History endpoints
    // - Admin / Super Admin: can read any user's history
    // - Others: only their own
    // -------------------------------------------------------------------------
    @GetMapping("/history/{userId}")
    public List<ClockResponseDTO> getHistory(@PathVariable Long userId, HttpServletRequest request) {
        Authentication auth = securityUtil.getAuthenticationOrThrow();

        boolean isAdmin = securityUtil.hasAdminRole(auth);
        if (!isAdmin) {
            Long tokenUserId = securityUtil.extractUserIdFromRequestToken(request);
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
        Long userId = securityUtil.extractUserIdFromRequestToken(request);
        return clockService.getHistory(userId)
                .stream()
                .map(responseMapper::toDTO)
                .toList();
    }

    @GetMapping("/status/me")
    public Map<String, Object> getMyCurrentStatus(HttpServletRequest request) {
        Long userId = securityUtil.extractUserIdFromRequestToken(request);
        var latestRecord = clockService.getHistory(userId)
                .stream()
                .findFirst();
        
        boolean isClockedIn = latestRecord
                .map(record -> record.getType() == ClockRecordType.IN)
                .orElse(false);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("isClockedIn", isClockedIn);

        latestRecord.ifPresent(record -> {
            response.put("lastClockType", record.getType().toString());
            response.put("lastClockTime", record.getRecordedAt().toString());
        });

        return response;
    }

    @GetMapping("/hours/me")
    public WorkHoursDTO getMyWorkHours(HttpServletRequest request) {
        Long userId = securityUtil.extractUserIdFromRequestToken(request);
        return workHoursService.calculateWorkHours(userId);
    }

    @GetMapping("/activity/me")
    public List<ClockResponseDTO> getMyTodayActivities(HttpServletRequest request) {
        Long userId = securityUtil.extractUserIdFromRequestToken(request);

        return clockService.getTodayActivities(userId)
                .stream()
                .map(responseMapper::toDTO)
                .toList();
    }

    // -------------------------------------------------------------------------
    // QR-based endpoints (employee flow, initiated from kiosk)
    // -------------------------------------------------------------------------

    @GetMapping("/qr/generate/in/{publicId}")
    public ClockQRCodeResponseDTO generateClockInQRCode(@PathVariable UUID publicId) {
        securityUtil.requireKioskOrAdmin();
        return qrCodeService.generateClockInQRCode(publicId);
    }

    @GetMapping("/qr/generate/out/{publicId}")
    public ClockQRCodeResponseDTO generateClockOutQRCode(@PathVariable UUID publicId) {
        securityUtil.requireKioskOrAdmin();
        return qrCodeService.generateClockOutQRCode(publicId);
    }

    // -------------------------------------------------------------------------
    // Clock in/out endpoints (employee flow, initiated from user device)
    // -------------------------------------------------------------------------

    @PostMapping("/qr/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockInWithQR(@RequestBody @Valid ClockQRCodeRequestDTO request,
                                          HttpServletRequest httpRequest) {
        String ipAddress = securityUtil.getClientIp(httpRequest);
        if (!rateLimitService.allowClockQrAttempt(ipAddress)) {
            throw new RateLimitException("Too many QR code clock-in attempts. Please try again later.");
        }

        Long authenticatedUserId = securityUtil.extractUserIdFromRequestToken(httpRequest);
        ClockRecord record = clockService.clockInWithQR(request.getToken(), authenticatedUserId);
        return responseMapper.toDTO(record);
    }

    @PostMapping("/qr/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockOutWithQR(@RequestBody @Valid ClockQRCodeRequestDTO request,
                                           HttpServletRequest httpRequest) {
        String ipAddress = securityUtil.getClientIp(httpRequest);
        if (!rateLimitService.allowClockQrAttempt(ipAddress)) {
            throw new RateLimitException("Too many QR code clock-out attempts. Please try again later.");
        }

        Long authenticatedUserId = securityUtil.extractUserIdFromRequestToken(httpRequest);
        ClockRecord record = clockService.clockOutWithQR(request.getToken(), authenticatedUserId);
        return responseMapper.toDTO(record);
    }

    // -------------------------------------------------------------------------
    // Admin manual clocking: recordedAtTimestamp + reason
    // -------------------------------------------------------------------------

    @PostMapping("/admin/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO adminClockIn(@RequestBody @Valid AdminClockRequestDTO request) {
        securityUtil.requireAdmin();
        ClockRecord record = clockService.adminClockIn(
                request.userId(),
                request.recordedAtTimestamp(),
                request.reason()
        );
        return responseMapper.toDTO(record);
    }

    @PostMapping("/admin/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO adminClockOut(@RequestBody @Valid AdminClockRequestDTO request) {
        securityUtil.requireAdmin();
        ClockRecord record = clockService.adminClockOut(
                request.userId(),
                request.recordedAtTimestamp(),
                request.reason()
        );
        return responseMapper.toDTO(record);
    }


    // -------------------------------------------------------------------------
    // Server-Sent Events for QR Scan Notifications
    // -------------------------------------------------------------------------
    @GetMapping(
            value = "/qr/stream/{tokenId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamQr(@PathVariable String tokenId, HttpServletRequest request) {
        securityUtil.requireKioskOrAdmin();
        return qrScanPushService.subscribe(tokenId);
    }
}
