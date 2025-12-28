package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.dto.request.AdminClockRequest;
import be.ahm282.QuickClock.application.dto.request.ClockQRCodeRequest;
import be.ahm282.QuickClock.application.dto.response.ClockQRCodeResponse;
import be.ahm282.QuickClock.application.dto.response.ClockResponse;
import be.ahm282.QuickClock.application.dto.response.WorkHoursResponse;
import be.ahm282.QuickClock.application.services.ClockService;
import be.ahm282.QuickClock.application.services.QRCodeService;
import be.ahm282.QuickClock.application.services.WorkHoursService;
import be.ahm282.QuickClock.domain.exception.RateLimitException;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import be.ahm282.QuickClock.infrastructure.mapper.ClockResponseMapper;
import be.ahm282.QuickClock.infrastructure.security.SecurityUtil;
import be.ahm282.QuickClock.infrastructure.security.service.RateLimitService;
import be.ahm282.QuickClock.infrastructure.adapters.out.notification.sse.QrScanPushService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ClockResponseMapper responseMapper;
    private final RateLimitService rateLimitService;
    private final SecurityUtil securityUtil;
    private final QrScanPushService qrScanPushService;
    private final WorkHoursService workHoursService;

    public ClockController(ClockService clockService,
                           QRCodeService qrCodeService,
                           ClockResponseMapper responseMapper,
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
    public ClockResponse clockIn(HttpServletRequest request) {
        Long userId = securityUtil.extractUserIdFromRequestToken(request);
        ClockRecord record = clockService.clockIn(userId);
        return responseMapper.toDTO(record);
    }

    @PostMapping("/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponse clockOut(HttpServletRequest request) {
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
    public List<ClockResponse> getHistory(@PathVariable Long userId, HttpServletRequest request) {
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
    public List<ClockResponse> getMyHistory(HttpServletRequest request) {
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
    public WorkHoursResponse getMyWorkHours(HttpServletRequest request) {
        Long userId = securityUtil.extractUserIdFromRequestToken(request);
        return workHoursService.calculateWorkHours(userId);
    }

    @GetMapping("/activity/me")
    public List<ClockResponse> getMyTodayActivities(HttpServletRequest request) {
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
    public ClockQRCodeResponse generateClockInQRCode(@PathVariable UUID publicId) {
        securityUtil.requireKioskOrAdmin();
        return qrCodeService.generateClockInQRCode(publicId);
    }

    @GetMapping("/qr/generate/out/{publicId}")
    public ClockQRCodeResponse generateClockOutQRCode(@PathVariable UUID publicId) {
        securityUtil.requireKioskOrAdmin();
        return qrCodeService.generateClockOutQRCode(publicId);
    }

    // -------------------------------------------------------------------------
    // Clock in/out endpoints (employee flow, initiated from user device)
    // -------------------------------------------------------------------------

    @PostMapping("/qr/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponse clockInWithQR(@RequestBody @Valid ClockQRCodeRequest request,
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
    public ClockResponse clockOutWithQR(@RequestBody @Valid ClockQRCodeRequest request,
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
    public ClockResponse adminClockIn(@RequestBody @Valid AdminClockRequest request) {
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
    public ClockResponse adminClockOut(@RequestBody @Valid AdminClockRequest request) {
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
    public ResponseEntity<SseEmitter> streamQr(@PathVariable String tokenId, HttpServletRequest request) {
        securityUtil.requireKioskOrAdmin();
        SseEmitter emitter = qrScanPushService.subscribe(tokenId);

        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no")
                .body(emitter);
    }
}
