package be.ahm282.QuickClock.domain.model;

import be.ahm282.QuickClock.domain.exception.ValidationException;
import java.time.LocalDateTime;

/**
 * Domain model for TimeEntry.
 */
public class ClockRecord {

    private Long id;
    private Long userId;
    private String type;
    private LocalDateTime timestamp;

    // Private constructor for persistence mapping
    public ClockRecord() {}

    public ClockRecord(Long id, Long userId,  String type, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.timestamp = timestamp;
        this.validate();
    }

    public void validate() {
        if (userId == null) {
            throw new ValidationException("TimeEntry must be associated with a user.");
        }

        if (type == null) {
            throw new ValidationException("TimeEntry must be associated with a type.");
        }

        if (timestamp == null) {
            throw new ValidationException("TimeEntry must be associated with a timestamp.");
        }

        if (!type.equals("IN")  && !type.equals("OUT")) {
            throw new ValidationException("Type must be either IN or OUT.");
        }
    }

    public static ClockRecord create(Long userId, String type) {
        ClockRecord record = new ClockRecord(null, userId, type, LocalDateTime.now());
        record.validate();
        return record;
    }

    public static ClockRecord fromEntity(Long id, Long userId, String type, LocalDateTime timestamp) {
        ClockRecord record = new ClockRecord();
        record.id = id;
        record.userId = userId;
        record.type = type;
        record.timestamp = timestamp;
        return record;
    }

    // --- Getters && Setters ---
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
