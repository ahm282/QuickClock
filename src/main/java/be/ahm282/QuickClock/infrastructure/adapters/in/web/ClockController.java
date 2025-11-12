package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.ports.in.ClockUseCase;
import be.ahm282.QuickClock.application.ports.in.dto.ClockRequestDTO;
import be.ahm282.QuickClock.application.ports.in.dto.ClockResponseDTO;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.mapper.ClockResponseMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clock")
public class ClockController {
    private final ClockUseCase clockService;
    private final ClockResponseMapper responseMapper;

    public ClockController(ClockUseCase clockService, ClockResponseMapper responseMapper) {
        this.clockService = clockService;
        this.responseMapper = responseMapper;
    }

    @PostMapping("/in")
    @ResponseStatus(HttpStatus.CREATED)
    public ClockResponseDTO clockIn(@RequestBody  @Valid ClockRequestDTO requestDTO) {
        ClockRecord record =  clockService.clockIn(requestDTO.getUserId());
        return responseMapper.toDTO(record);
    }

    @PostMapping("/out")
    public ClockResponseDTO clockOut(@RequestBody @Valid ClockRequestDTO requestDTO) {
        ClockRecord record = clockService.clockOut(requestDTO.getUserId());
        return responseMapper.toDTO(record);
    }

    @GetMapping("/history/{userId}")
    public List<ClockResponseDTO> getHistory(@PathVariable @Valid Long userId) {
        return clockService.getHistory(userId)
                .stream()
                .map(responseMapper::toDTO)
                .toList();
    }
}
