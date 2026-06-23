package com.example.integrationplatform.repository;

import com.example.integrationplatform.entity.WebhookEvent;
import com.example.integrationplatform.enums.WebhookEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByEventId(String eventId);

    List<WebhookEvent> findByStatus(WebhookEventStatus status);

    List<WebhookEvent> findTop50ByStatusOrderByCreatedAtAsc(WebhookEventStatus status);

    List<WebhookEvent> findTop50ByStatusOrderByPublishedAtAsc(WebhookEventStatus status);

    Optional<WebhookEvent> findBySourceAndEventId(String source, String eventId);

    List<WebhookEvent> findTop50ByStatusAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
            WebhookEventStatus status,
            LocalDateTime processingStartedAt
    );
}