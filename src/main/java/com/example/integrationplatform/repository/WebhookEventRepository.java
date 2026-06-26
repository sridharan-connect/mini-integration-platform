package com.example.integrationplatform.repository;

import com.example.integrationplatform.entity.WebhookEvent;
import com.example.integrationplatform.enums.WebhookEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByEventId(String eventId);

    List<WebhookEvent> findByStatus(WebhookEventStatus status);

    //List<WebhookEvent> findTop50ByStatusOrderByCreatedAtAsc(WebhookEventStatus status);

    @Query("""
        SELECT e
        FROM WebhookEvent e
        WHERE e.status = :status
          AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
        ORDER BY e.createdAt ASC
        """)
    List<WebhookEvent> findPublishableEvents(
            @Param("status") WebhookEventStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    //List<WebhookEvent> findTop50ByStatusOrderByPublishedAtAsc(WebhookEventStatus status);

    @Query("""
        SELECT e
        FROM WebhookEvent e
        WHERE e.status = :status
          AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
        ORDER BY e.publishedAt ASC
        """)
    List<WebhookEvent> findProcessableEvents(
            @Param("status") WebhookEventStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    Optional<WebhookEvent> findBySourceAndEventId(String source, String eventId);

    List<WebhookEvent> findTop50ByStatusAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
            WebhookEventStatus status,
            LocalDateTime processingStartedAt
    );
}