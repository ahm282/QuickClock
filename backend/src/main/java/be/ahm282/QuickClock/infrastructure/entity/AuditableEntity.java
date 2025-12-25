package be.ahm282.QuickClock.infrastructure.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        prePersist();
    }

    @PreUpdate
    protected void onUpdate() {
        preUpdate();
    }

    protected void prePersist() {}
    protected void preUpdate() {}

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        throw new UnsupportedOperationException("createdAt is managed automatically");
    }

    protected void setUpdatedAt(Instant updatedAt) {
        throw new UnsupportedOperationException("updatedAt is managed automatically");
    }
}
