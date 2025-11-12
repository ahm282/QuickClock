package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeRequestDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeResponseDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockResponseDTO;
import be.ahm282.QuickClock.application.services.ClockService;
import be.ahm282.QuickClock.application.services.QRCodeService;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.mapper.ClockResponseMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clock/qr")
public class ClockQRCodeController {
    private final QRCodeService qrCodeService;
    private final ClockService clockService;
    private final ClockResponseMapper responseMapper;

    public ClockQRCodeController(QRCodeService qrCodeService,  ClockService clockService, ClockResponseMapper responseMapper) {
        this.qrCodeService = qrCodeService;
        this.clockService = clockService;
        this.responseMapper = responseMapper;
    }

    @GetMapping("/generate/{userId}")
    public ClockQRCodeResponseDTO generateQRCode(@PathVariable Long userId) {
        return qrCodeService.generateQRCode(userId);
    }

    // TODO: Should be PostMapping
    @GetMapping("/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockInWithQR(@Valid @RequestParam String token) {
        ClockRecord record = clockService.clockInWithQR(token);
        return responseMapper.toDTO(record);
    }

    @GetMapping("/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockOutWithQR(@Valid @RequestParam String token) {
        ClockRecord record = clockService.clockOutWithQR(token);
        return responseMapper.toDTO(record);
    }
}
