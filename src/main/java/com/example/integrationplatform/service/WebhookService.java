package com.example.integrationplatform.service;

import com.example.integrationplatform.dto.WebhookRequest;
import com.example.integrationplatform.dto.WebhookResponse;
import com.example.integrationplatform.entity.WebhookEvent;
import com.example.integrationplatform.enums.WebhookEventStatus;
import com.example.integrationplatform.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private final WebhookEventRepository webhookEventRepository;

    public WebhookService(WebhookEventRepository webhookEventRepository) {
        this.webhookEventRepository = webhookEventRepository;
    }

    @Transactional
    public WebhookResponse receiveWebhook(WebhookRequest request) {
        WebhookEvent event = new WebhookEvent();

        event.setEventId(request.getEventId());
        event.setEventType(request.getEventType());
        event.setSource(request.getSource());
        event.setPayload(request.getPayload().toString());
        event.setStatus(WebhookEventStatus.PENDING_PUBLISH);
        event.setRetryCount(0);

        WebhookEvent savedEvent = webhookEventRepository.save(event);

        return new WebhookResponse(
                savedEvent.getEventId(),
                savedEvent.getStatus().name()
        );
    }
}