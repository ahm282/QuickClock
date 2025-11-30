package be.ahm282.QuickClock.application.ports.in.dto;

import jakarta.validation.constraints.NotNull;

public class ClockRequestDTO {
    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "TimeEntry must be associated with a type")
    private String type;

    public ClockRequestDTO() {}

    public ClockRequestDTO(Long userId, String type) {
        this.userId = userId;
        this.type = type;
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
}
