package com.example.integrationplatform.dto;

public class WebhookResponse {

    private String eventId;
    private String status;

    public WebhookResponse(String eventId, String status) {
        this.eventId = eventId;
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }
}