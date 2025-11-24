package be.ahm282.QuickClock.domain.model;

import be.ahm282.QuickClock.domain.exception.ValidationException;

import java.time.Instant;

/**
 * Domain model for TimeEntry.
 */
public class ClockRecord {

    private Long id;
    private Long userId;
    private ClockRecordType type;
    private Instant timestamp;
    private  String reason;

    // Private constructor for persistence mapping
    public ClockRecord() {}

    public ClockRecord(Long id, Long userId,  ClockRecordType type, Instant timestamp, String reason) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.timestamp = timestamp;
        this.reason = reason;
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

        if (type != ClockRecordType.IN && type != ClockRecordType.OUT) {
            throw new ValidationException("Type must be either IN or OUT.");
        }
    }

    public static ClockRecord create(Long userId, ClockRecordType type) {
        ClockRecord record = new ClockRecord(null, userId, type, Instant.now(), null);
        record.validate();
        return record;
    }

    public static ClockRecord createAt(Long userId, ClockRecordType type, Instant timestamp, String reason) {
        Instant effectiveTimestamp = (timestamp != null) ? timestamp : Instant.now();
        return new ClockRecord(null, userId, type, effectiveTimestamp, reason);
    }

    public static ClockRecord fromEntity(Long id, Long userId, ClockRecordType type, Instant timestamp) {
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

    public Long getUserId() {
        return userId;
    }

    public ClockRecordType getType() {
        return type;
    }

    public Instant getTimestamp() { return timestamp; }

    public  String getReason() { return reason; }
}
