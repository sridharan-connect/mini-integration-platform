package com.example.integrationplatform.dto;

public class RetryWebhookEventResponse {

    private Long id;
    private String eventId;
    private String previousStatus;
    private String currentStatus;
    private String message;

    public RetryWebhookEventResponse(
            Long id,
            String eventId,
            String previousStatus,
            String currentStatus,
            String message
    ) {
        this.id = id;
        this.eventId = eventId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getMessage() {
        return message;
    }
}