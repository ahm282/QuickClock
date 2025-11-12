package be.ahm282.QuickClock.application.ports.in.dto;

import java.time.LocalDateTime;

public class ClockResponseDTO {
    private Long id;
    private Long userId;
    private String type;
    private LocalDateTime timestamp;

    public ClockResponseDTO() {}

    public ClockResponseDTO(Long id, Long userId, String type, LocalDateTime timestamp) {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
