package com.example.integrationplatform.dto;

public class WebhookResponse {

    private String eventId;
    private String status;
    private String message;
    private boolean duplicate;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public WebhookResponse(String eventId, String status, String message, boolean duplicate) {
        this.eventId = eventId;
        this.status = status;
        this.message = message;
        this.duplicate = duplicate;
    }
    public static WebhookResponse accepted(String eventId, String status) {
        return new WebhookResponse(
                eventId,
                status,
                "Webhook event accepted",
                false
        );
    }
    public static WebhookResponse duplicate(String eventId, String status) {
        return new WebhookResponse(
                eventId,
                status,
                "Duplicate event already received",
                true
        );
    }

    public String getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }
}