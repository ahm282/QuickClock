package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeRequestDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockQRCodeResponseDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockRequestDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockResponseDTO;
import be.ahm282.QuickClock.application.services.ClockService;
import be.ahm282.QuickClock.application.services.QRCodeService;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.mapper.ClockResponseDTOMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clock")
public class ClockController {

    private final ClockService clockService;
    private final QRCodeService qrCodeService;
    private final ClockResponseDTOMapper responseMapper;

    public ClockController(ClockService clockService,
                           QRCodeService qrCodeService,
                           ClockResponseDTOMapper responseMapper) {
        this.clockService = clockService;
        this.qrCodeService = qrCodeService;
        this.responseMapper = responseMapper;
    }

    // ---------- ID-based endpoints ----------
    @PostMapping("/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockIn(@RequestBody @Valid ClockRequestDTO requestDTO) {
        ClockRecord record = clockService.clockIn(requestDTO.getUserId());
        return responseMapper.toDTO(record);
    }

    @PostMapping("/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockOut(@RequestBody @Valid ClockRequestDTO requestDTO) {
        ClockRecord record = clockService.clockOut(requestDTO.getUserId());
        return responseMapper.toDTO(record);
    }

    @GetMapping("/history/{userId}")
    public List<ClockResponseDTO> getHistory(@PathVariable Long userId) {
        return clockService.getHistory(userId)
                .stream()
                .map(responseMapper::toDTO)
                .toList();
    }

    // ---------- QR-based endpoints ----------
    @GetMapping("/qr/generate/in/{userId}")
    public ClockQRCodeResponseDTO generateClockInQRCode(@PathVariable Long userId) {
        return qrCodeService.generateClockInQRCode(userId);
    }

    @GetMapping("/qr/generate/out/{userId}")
    public ClockQRCodeResponseDTO generateClockOutQRCode(@PathVariable Long userId) {
        return qrCodeService.generateClockOutQRCode(userId);
    }

    @PostMapping("/qr/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockInWithQR(@RequestBody @Valid ClockQRCodeRequestDTO request) {
        ClockRecord record = clockService.clockInWithQR(request.getToken());
        return responseMapper.toDTO(record);
    }

    @PostMapping("/qr/out")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockOutWithQR(@RequestBody @Valid ClockQRCodeRequestDTO request) {
        ClockRecord record = clockService.clockOutWithQR(request.getToken());
        return responseMapper.toDTO(record);
    }
}
