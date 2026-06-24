package com.example.integrationplatform.entity;

import com.example.integrationplatform.enums.BusinessIdempotencyStatus;
import jakarta.persistence.*;


import java.time.LocalDateTime;

@Entity
@Table(
        name = "business_idempotency",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_business_idempotency_key", columnNames = "idempotency_key")
        }
)
public class BusinessIdempotency {

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public BusinessIdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(BusinessIdempotencyStatus status) {
        this.status = status;
    }

    public Long getWebhookEventId() {
        return webhookEventId;
    }

    public void setWebhookEventId(Long webhookEventId) {
        this.webhookEventId = webhookEventId;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "payload_hash")
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BusinessIdempotencyStatus status;

    @Column(name = "webhook_event_id", nullable = false)
    private Long webhookEventId;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static BusinessIdempotency start(
            String idempotencyKey,
            String source,
            String eventType,
            String businessId,
            String eventId,
            Long webhookEventId
    ) {
        LocalDateTime now = LocalDateTime.now();

        BusinessIdempotency businessIdempotency = new BusinessIdempotency();
        businessIdempotency.idempotencyKey = idempotencyKey;
        businessIdempotency.source = source;
        businessIdempotency.eventType = eventType;
        businessIdempotency.businessId = businessId;
        businessIdempotency.eventId = eventId;
        businessIdempotency.webhookEventId = webhookEventId;
        businessIdempotency.status = BusinessIdempotencyStatus.IN_PROGRESS;
        businessIdempotency.createdAt = now;
        businessIdempotency.updatedAt = now;

        return businessIdempotency;
    }

    public void markInProgress() {
        this.status = BusinessIdempotencyStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = BusinessIdempotencyStatus.COMPLETED;
        this.lastError = null;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = BusinessIdempotencyStatus.FAILED;
        this.lastError = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}
