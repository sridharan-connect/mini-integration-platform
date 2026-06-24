package com.example.integrationplatform.service;

import com.example.integrationplatform.entity.WebhookEvent;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class BusinessIdempotencyKeyService {

    private final ObjectMapper objectMapper;

    public BusinessIdempotencyKeyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildKey(WebhookEvent event) {
        String source = getRequiredValue(event.getSource(), "source");
        String eventType = getRequiredValue(event.getEventType(), "eventType");
        String orderId = extractOrderId(event);

        if ("ORDER_CREATED".equals(eventType)) {
            return source + ":" + eventType + ":" + orderId;
        }

        if ("ORDER_UPDATED".equals(eventType)) {
            return source + ":" + eventType + ":" + orderId + ":" + event.getEventId();
        }

        return source + ":" + eventType + ":" + orderId + ":" + event.getEventId();
    }

    private String extractOrderId(WebhookEvent event) {
        JsonNode payload = readPayload(event);
        return getRequiredText(payload, "orderId");
    }

    private JsonNode readPayload(WebhookEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to parse webhook event payload. eventId=" + event.getEventId(),
                    ex
            );
        }
    }

    private String getRequiredText(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);

        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required payload field: " + fieldName);
        }

        return value.asText();
    }

    private String getRequiredValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }

        return value;
    }
}