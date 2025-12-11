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
    private Instant recordedAt;
    private  String reason;

    // Private constructor for persistence mapping
    public ClockRecord() {}

    public ClockRecord(Long id, Long userId, ClockRecordType type, Instant recordedAt, String reason) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.recordedAt = recordedAt;
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

        if (recordedAt == null) {
            throw new ValidationException("TimeEntry must be associated with a recordedAt timestamp.");
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

    public static ClockRecord createAt(Long userId, ClockRecordType type, Instant recordedAtTimestamp, String reason) {
        Instant effectiveTimestamp = (recordedAtTimestamp != null) ? recordedAtTimestamp : Instant.now();
        return new ClockRecord(null, userId, type, effectiveTimestamp, reason);
    }

    public static ClockRecord fromEntity(Long id, Long userId, ClockRecordType type, Instant recordedAtTimestamp) {
        ClockRecord record = new ClockRecord();
        record.id = id;
        record.userId = userId;
        record.type = type;
        record.recordedAt = recordedAtTimestamp;
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

    public Instant getRecordedAt() { return recordedAt; }

    public  String getReason() { return reason; }
}
