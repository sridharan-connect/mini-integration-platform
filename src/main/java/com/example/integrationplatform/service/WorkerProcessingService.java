package com.example.integrationplatform.service;

import com.example.integrationplatform.entity.BusinessIdempotency;
import com.example.integrationplatform.entity.WebhookEvent;
import com.example.integrationplatform.enums.BusinessIdempotencyStatus;
import com.example.integrationplatform.enums.WebhookEventStatus;
import com.example.integrationplatform.repository.BusinessIdempotencyRepository;
import com.example.integrationplatform.repository.WebhookEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WorkerProcessingService {

    private static final Logger logger =
            LoggerFactory.getLogger(WorkerProcessingService.class);

    private static final int MAX_PROCESSING_RETRY = 3;

    private final WebhookEventRepository webhookEventRepository;
    private final MockExternalApiService mockExternalApiService;
    private final BusinessIdempotencyKeyService businessIdempotencyKeyService;
    private final BusinessIdempotencyRepository businessIdempotencyRepository;
    private final ObjectMapper objectMapper;

    public WorkerProcessingService(
            WebhookEventRepository webhookEventRepository,
            MockExternalApiService mockExternalApiService,
            BusinessIdempotencyKeyService businessIdempotencyKeyService,
            BusinessIdempotencyRepository businessIdempotencyRepository,
            ObjectMapper objectMapper
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.mockExternalApiService = mockExternalApiService;
        this.businessIdempotencyKeyService = businessIdempotencyKeyService;
        this.businessIdempotencyRepository = businessIdempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 7000)
    public void processPublishedEvents() {
        List<WebhookEvent> events =
                webhookEventRepository.findTop50ByStatusOrderByPublishedAtAsc(
                        WebhookEventStatus.PUBLISHED
                );

        if (events.isEmpty()) {
            return;
        }

        logger.info("Found {} published webhook events to process", events.size());

        for (WebhookEvent event : events) {
            processEvent(event);
        }
    }

    private void processEvent(WebhookEvent event) {
        BusinessIdempotency businessIdempotency = null;

        try {
            // Mark webhook event as PROCESSING
            event.setStatus(WebhookEventStatus.PROCESSING);
            event.setProcessingStartedAt(LocalDateTime.now());
            event.setUpdatedAt(LocalDateTime.now());
            webhookEventRepository.save(event);

            logger.info("Webhook event processing started. eventId={}",
                    event.getEventId());

            // Build business idempotency key
            String idempotencyKey = businessIdempotencyKeyService.buildKey(event);

            logger.info("Business idempotency key generated. eventId={}, key={}",
                    event.getEventId(),
                    idempotencyKey);

            Optional<BusinessIdempotency> existing =
                    businessIdempotencyRepository.findByIdempotencyKey(idempotencyKey);

            // If this business action is already completed, skip external API call
            if (existing.isPresent()
                    && existing.get().getStatus() == BusinessIdempotencyStatus.COMPLETED) {

                logger.info("Business action already completed. Skipping external API. eventId={}, key={}",
                        event.getEventId(),
                        idempotencyKey);

                event.markProcessed();
                webhookEventRepository.save(event);

                return;
            }

            // Existing FAILED / IN_PROGRESS can be retried.
            // If no record exists, create a new business idempotency record.
            businessIdempotency = existing.orElseGet(() ->
                    BusinessIdempotency.start(
                            idempotencyKey,
                            event.getSource(),
                            extractEventType(event),
                            extractBusinessId(event),
                            event.getEventId(),
                            event.getId()
                    )
            );

            businessIdempotency.markInProgress();
            businessIdempotencyRepository.save(businessIdempotency);

            // Call external API outside any long DB transaction
            mockExternalApiService.process(event);

            // Mark business action as COMPLETED
            businessIdempotency.markCompleted();
            businessIdempotencyRepository.save(businessIdempotency);

            // Mark webhook event as PROCESSED
            event.markProcessed();
            webhookEventRepository.save(event);

            logger.info("Webhook event processed successfully. eventId={}, key={}",
                    event.getEventId(),
                    idempotencyKey);

        } catch (Exception ex) {
            handleProcessingFailure(event, businessIdempotency, ex);
        }
    }

    private void handleProcessingFailure(
            WebhookEvent event,
            BusinessIdempotency businessIdempotency,
            Exception ex
    ) {
        int retryCount = event.getProcessingRetryCount() + 1;

        event.setProcessingRetryCount(retryCount);
        event.setProcessingLastError(ex.getMessage());
        event.setUpdatedAt(LocalDateTime.now());

        if (retryCount >= MAX_PROCESSING_RETRY) {
            event.setStatus(WebhookEventStatus.DLQ);
            event.setDlqReason(ex.getMessage());

            logger.error("Webhook event moved to DLQ. eventId={}, retryCount={}, error={}",
                    event.getEventId(),
                    retryCount,
                    ex.getMessage());
        } else {
            event.setStatus(WebhookEventStatus.PUBLISHED);

            logger.warn("Webhook event processing failed. Retrying later. eventId={}, retryCount={}, error={}",
                    event.getEventId(),
                    retryCount,
                    ex.getMessage());
        }

        webhookEventRepository.save(event);

        if (businessIdempotency != null) {
            businessIdempotency.markFailed(ex.getMessage());
            businessIdempotencyRepository.save(businessIdempotency);
        }
    }

    private String extractEventType(WebhookEvent event) {
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            throw new IllegalArgumentException("Missing required field: eventType");
        }

        return event.getEventType();
    }

    private String extractBusinessId(WebhookEvent event) {
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
}