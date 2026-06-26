package com.example.integrationplatform.service;

import com.example.integrationplatform.entity.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockExternalApiService {

    private static final Logger logger =
            LoggerFactory.getLogger(MockExternalApiService.class);

    public void process(WebhookEvent event) {
//        logger.info("Calling mock external API. eventId={}, eventType={}",
//                event.getEventId(),
//                event.getEventType());
        throw new RuntimeException("External API unavailable");

        // For now, success case.
        // Later we can throw exception here to test DLQ flow.
    }
}
