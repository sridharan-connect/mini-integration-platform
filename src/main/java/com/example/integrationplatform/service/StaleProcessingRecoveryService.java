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
public class StaleProcessingRecoveryService {

    private static final Logger logger =
            LoggerFactory.getLogger(StaleProcessingRecoveryService.class);

    private static final int MAX_PROCESSING_RETRY = 3;
    private static final int STALE_PROCESSING_MINUTES = 10;

    private final WebhookEventRepository webhookEventRepository;

    public StaleProcessingRecoveryService(WebhookEventRepository webhookEventRepository) {
        this.webhookEventRepository = webhookEventRepository;
    }

    @Scheduled(fixedDelay = 60000)
    public void recoverStaleProcessingEvents() {
        LocalDateTime cutoffTime =
                LocalDateTime.now().minusMinutes(STALE_PROCESSING_MINUTES);

        List<WebhookEvent> staleEvents =
                webhookEventRepository
                        .findTop50ByStatusAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
                                WebhookEventStatus.PROCESSING,
                                cutoffTime
                        );

        if (staleEvents.isEmpty()) {
            return;
        }

        logger.warn("Found {} stale PROCESSING webhook events for recovery",
                staleEvents.size());

        for (WebhookEvent event : staleEvents) {
            recoverEvent(event);
        }
    }

    private void recoverEvent(WebhookEvent event) {
        int previousRetryCount = event.getProcessingRetryCount() == null
                ? 0
                : event.getProcessingRetryCount();

        event.recoverStaleProcessing(MAX_PROCESSING_RETRY);

        WebhookEvent savedEvent = webhookEventRepository.save(event);

        if (savedEvent.getStatus() == WebhookEventStatus.DLQ) {
            logger.error(
                    "Stale PROCESSING event moved to DLQ. eventId={}, previousRetryCount={}, currentRetryCount={}",
                    savedEvent.getEventId(),
                    previousRetryCount,
                    savedEvent.getProcessingRetryCount()
            );
        } else {
            logger.warn(
                    "Stale PROCESSING event recovered for retry. eventId={}, previousRetryCount={}, currentRetryCount={}, newStatus={}, nextRetryAt={}",
                    savedEvent.getEventId(),
                    previousRetryCount,
                    savedEvent.getProcessingRetryCount(),
                    savedEvent.getStatus(),
                    savedEvent.getNextRetryAt()
            );
        }
    }
}