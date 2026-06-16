package com.example.integrationplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public class WebhookRequest {

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "eventType is required")
    private String eventType;

    @NotBlank(message = "source is required")
    private String source;

    @NotNull(message = "payload is required")
    private JsonNode payload;

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

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}