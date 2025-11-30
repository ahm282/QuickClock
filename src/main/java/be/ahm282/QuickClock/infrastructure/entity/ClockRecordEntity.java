package be.ahm282.QuickClock.infrastructure.entity;

import be.ahm282.QuickClock.domain.model.ClockRecordType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "clock_records",
        indexes = {
            @Index(name = "idx_clock_user", columnList = "user_id"),
            @Index(name = "idx_clock_user_ts", columnList = "user_id, timestamp")
        }
)
public class ClockRecordEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private ClockRecordType type;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    @Column(name = "reason", length = 255)
    private String reason;

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
