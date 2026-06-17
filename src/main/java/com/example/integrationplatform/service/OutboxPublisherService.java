package com.example.integrationplatform.service;

import com.example.integrationplatform.entity.WebhookEvent;
import com.example.integrationplatform.enums.WebhookEventStatus;
import com.example.integrationplatform.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxPublisherService {

    private static final Logger logger =
            LoggerFactory.getLogger(OutboxPublisherService.class);

    private static final int MAX_RETRY = 3;

    private final WebhookEventRepository webhookEventRepository;
    private final EventPublisherService eventPublisherService;

    public OutboxPublisherService(
            WebhookEventRepository webhookEventRepository,
            EventPublisherService eventPublisherService
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.eventPublisherService = eventPublisherService;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
          List<WebhookEvent> pendingEvents =
                webhookEventRepository.findTop50ByStatusOrderByCreatedAtAsc(
                        WebhookEventStatus.PENDING_PUBLISH
                );

        if (pendingEvents.isEmpty()) {
            return;
        }

        logger.info("Found {} pending webhook events to publish", pendingEvents.size());

        for (WebhookEvent event : pendingEvents) {
            try {
                eventPublisherService.publish(event);
                event.markPublished();

                logger.info("Webhook event published successfully. eventId={}",
                        event.getEventId());

            } catch (Exception ex) {
                event.recordPublishFailure(ex.getMessage(), MAX_RETRY);

                logger.error("Webhook event publish failed. eventId={}, retryCount={}, error={}",
                        event.getEventId(),
                        event.getRetryCount(),
                        ex.getMessage());
            }

            webhookEventRepository.save(event);
        }
    }
}