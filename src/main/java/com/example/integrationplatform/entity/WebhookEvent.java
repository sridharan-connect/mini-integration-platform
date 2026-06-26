package com.example.integrationplatform.entity;

import com.example.integrationplatform.enums.WebhookEventStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_webhook_source_event_id",
                        columnNames = {"source", "event_id"}
                )
        },
        indexes = {
                @Index(name = "idx_webhook_status_created_at", columnList = "status, created_at"),
                @Index(name = "idx_webhook_status_next_retry_created", columnList = "status, next_retry_at, created_at"),
                @Index(name = "idx_webhook_status_next_retry_published", columnList = "status, next_retry_at, published_at"),
                @Index(name = "idx_webhook_status_processing_started", columnList = "status, processing_started_at")
        }
)
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WebhookEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "processing_retry_count")
    private Integer processingRetryCount = 0;

    @Column(name = "processing_last_error")
    private String processingLastError;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "dlq_reason")
    private String dlqReason;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markPublished() {
        LocalDateTime now = LocalDateTime.now();

        this.status = WebhookEventStatus.PUBLISHED;
        this.publishedAt =now;
        this.lastError = null;
        this.setNextRetryAt(null);
        this.updatedAt=now;
    }

    public void markProcessed() {
        this.status = WebhookEventStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.processingLastError = null;
        this.dlqReason = null;
        this.setNextRetryAt(null);
        this.updatedAt = LocalDateTime.now();
    }

    public void recordPublishFailure(String errorMessage, int maxRetry) {
        this.retryCount = this.retryCount + 1;
        this.lastError = errorMessage;
        this.updatedAt = LocalDateTime.now();

        if (this.retryCount >= maxRetry) {
            this.status = WebhookEventStatus.PUBLISH_FAILED;
            this.setNextRetryAt(null);
        } else {
            this.status = WebhookEventStatus.PENDING_PUBLISH;
            this.setNextRetryAt(calculateNextRetryAt(this.retryCount));
        }
    }

    public void retryPublishing() {
        this.status = WebhookEventStatus.PENDING_PUBLISH;
        this.retryCount = 0;
        this.lastError = null;
        this.publishedAt = null;
        this.nextRetryAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void retryProcessingFromDlq() {
        this.status = WebhookEventStatus.PUBLISHED;
        this.processingRetryCount = 0;
        this.processingLastError = null;
        this.dlqReason = null;
        this.processingStartedAt = null;
        this.processedAt = null;
        this.nextRetryAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void recoverStaleProcessing(int maxRetry) {
        LocalDateTime now = LocalDateTime.now();

        int currentRetryCount = this.processingRetryCount == null
                ? 0
                : this.processingRetryCount;

        int nextRetryCount = currentRetryCount + 1;

        this.processingRetryCount = nextRetryCount;
        this.processingLastError = "Recovered from stale PROCESSING state";
        this.processingStartedAt = null;
        this.updatedAt = now;

        if (nextRetryCount >= maxRetry) {
            this.status = WebhookEventStatus.DLQ;
            this.dlqReason = "Moved to DLQ after stale PROCESSING recovery exceeded max retries";
            this.nextRetryAt = null;
        } else {
            this.status = WebhookEventStatus.PUBLISHED;
            this.dlqReason = null;
            this.nextRetryAt = calculateNextRetryAt(nextRetryCount);
        }
    }

    public LocalDateTime calculateNextRetryAt(int retryCount) {
        int delayMinutes;

        if (retryCount == 1) {
            delayMinutes = 1;
        } else if (retryCount == 2) {
            delayMinutes = 5;
        } else {
            delayMinutes = 15;
        }

        return LocalDateTime.now().plusMinutes(delayMinutes);
    }
    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public WebhookEventStatus getStatus() {
        return status;
    }

    public void setStatus(WebhookEventStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Integer getProcessingRetryCount() {
        return processingRetryCount;
    }

    public void setProcessingRetryCount(Integer processingRetryCount) {
        this.processingRetryCount = processingRetryCount;
    }

    public String getProcessingLastError() {
        return processingLastError;
    }

    public void setProcessingLastError(String processingLastError) {
        this.processingLastError = processingLastError;
    }

    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getDlqReason() {
        return dlqReason;
    }

    public void setDlqReason(String dlqReason) {
        this.dlqReason = dlqReason;
    }
}