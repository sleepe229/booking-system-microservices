package com.hotel.audit.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_log_event_type", columnList = "event_type"),
                @Index(name = "idx_audit_log_booking_id", columnList = "booking_id"),
                @Index(name = "idx_audit_log_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_log_customer_email", columnList = "customer_email"),
                @Index(name = "idx_audit_log_timestamp_event", columnList = "timestamp, event_type")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "timestamp", nullable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "booking_id")
    private String bookingId;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(String eventType, String eventData, String bookingId, String customerEmail) {
        this.eventType = eventType;
        this.eventData = eventData;
        this.bookingId = bookingId;
        this.customerEmail = customerEmail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", bookingId=" + bookingId +
                ", customerEmail='" + customerEmail + '\'' +
                '}';
    }
}