package be.ahm282.QuickClock.application.ports.in.dto;

import be.ahm282.QuickClock.domain.model.ClockRecordType;

import java.time.Instant;

public class ClockResponseDTO {
    private Long id;
    private Long userId;
    private ClockRecordType type;
    private Instant timestamp;

    public ClockResponseDTO() {}

    public ClockResponseDTO(Long id, Long userId, ClockRecordType type, Instant timestamp) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ClockRecordType getType() {
        return type;
    }

    public void setType(ClockRecordType type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
