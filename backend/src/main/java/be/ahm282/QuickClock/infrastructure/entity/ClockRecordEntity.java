package be.ahm282.QuickClock.infrastructure.entity;

import be.ahm282.QuickClock.domain.model.ClockRecordType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "clock_records",
        indexes = {
            @Index(name = "idx_clock_user", columnList = "user_id"),
            @Index(name = "idx_clock_user_ts", columnList = "user_id, recorded_at")
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

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
    @Column(name = "reason")
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

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
