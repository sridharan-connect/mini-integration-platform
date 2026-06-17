package com.example.integrationplatform.enums;

public enum WebhookEventStatus {
    RECEIVED,
    PENDING_PUBLISH,
    PUBLISHED,
    PUBLISH_FAILED,
    PROCESSING,
    PROCESSED,
    FAILED,
    DLQ
}