package com.example.integrationplatform.service;

import com.example.integrationplatform.dto.RetryWebhookEventResponse;
import com.example.integrationplatform.entity.WebhookEvent;
import com.example.integrationplatform.enums.WebhookEventStatus;
import com.example.integrationplatform.repository.WebhookEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WebhookEventRetryService {

    private final WebhookEventRepository webhookEventRepository;

    public WebhookEventRetryService(WebhookEventRepository webhookEventRepository) {
        this.webhookEventRepository = webhookEventRepository;
    }

    public RetryWebhookEventResponse retryEvent(Long id) {
        WebhookEvent event = webhookEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Webhook event not found"
                ));

        WebhookEventStatus previousStatus = event.getStatus();

        if (previousStatus == WebhookEventStatus.PUBLISH_FAILED) {
            event.retryPublishing();
        } else if (previousStatus == WebhookEventStatus.DLQ) {
            event.retryProcessingFromDlq();
        } else if (previousStatus == WebhookEventStatus.PROCESSED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Webhook event is already processed and cannot be retried"
            );
        } else if (
                previousStatus == WebhookEventStatus.PENDING_PUBLISH ||
                        previousStatus == WebhookEventStatus.PUBLISHED ||
                        previousStatus == WebhookEventStatus.PROCESSING ||
                        previousStatus == WebhookEventStatus.RECEIVED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Webhook event is already active and cannot be retried"
            );
        } else {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Webhook event cannot be retried from status: " + previousStatus
            );
        }

        WebhookEvent savedEvent = webhookEventRepository.save(event);

        return new RetryWebhookEventResponse(
                savedEvent.getId(),
                savedEvent.getEventId(),
                previousStatus.name(),
                savedEvent.getStatus().name(),
                "Webhook event moved for retry successfully"
        );
    }
}