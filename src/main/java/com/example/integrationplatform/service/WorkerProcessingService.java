package com.example.integrationplatform.service;

import com.example.integrationplatform.entity.WebhookEvent;
import com.example.integrationplatform.enums.WebhookEventStatus;
import com.example.integrationplatform.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkerProcessingService {

    private static final Logger logger =
            LoggerFactory.getLogger(WorkerProcessingService.class);

    private static final int MAX_PROCESSING_RETRY = 3;

    private final WebhookEventRepository webhookEventRepository;
    private final MockExternalApiService mockExternalApiService;

    public WorkerProcessingService(
            WebhookEventRepository webhookEventRepository,
            MockExternalApiService mockExternalApiService
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.mockExternalApiService = mockExternalApiService;
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
        try {
            event.setStatus(WebhookEventStatus.PROCESSING);
            event.setProcessingStartedAt(LocalDateTime.now());
            event.setUpdatedAt(LocalDateTime.now());
            webhookEventRepository.save(event);

            logger.info("Webhook event processing started. eventId={}",
                    event.getEventId());

            mockExternalApiService.process(event);

            event.setStatus(WebhookEventStatus.PROCESSED);
            event.setProcessedAt(LocalDateTime.now());
            event.setProcessingLastError(null);
            event.setDlqReason(null);
            event.setUpdatedAt(LocalDateTime.now());

            webhookEventRepository.save(event);

            logger.info("Webhook event processed successfully. eventId={}",
                    event.getEventId());

        } catch (Exception ex) {
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
        }
    }
}
