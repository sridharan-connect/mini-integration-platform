package com.example.integrationplatform.enums;

public enum WebhookEventStatus {
    RECEIVED,
    PENDING_PUBLISH,
    PUBLISHED,
    PROCESSING,
    PROCESSED,
    FAILED,
    DLQ
}