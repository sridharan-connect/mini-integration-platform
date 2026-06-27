package com.example.integrationplatform.service;

import com.example.integrationplatform.entity.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock-publisher")
public class MockEventPublisherService implements EventPublisherService {

    private static final Logger logger =
            LoggerFactory.getLogger(MockEventPublisherService.class);

    @Override
    public void publish(WebhookEvent event) {
        logger.info("Mock publishing webhook event. eventId={}, eventType={}, source={}",
                event.getEventId(),
                event.getEventType(),
                event.getSource());
    }
}