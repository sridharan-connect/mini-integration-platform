package com.example.integrationplatform.service;

import com.example.integrationplatform.entity.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class EventPublisherService {

    private static final Logger logger =
            LoggerFactory.getLogger(EventPublisherService.class);


    public void publish(WebhookEvent event) {
        //throw new RuntimeException("Kafka unavailable");
        logger.info("Publishing webhook event. eventId={}, eventType={}, source={}",
                event.getEventId(),
                event.getEventType(),
                event.getSource());

        // Later this will be replaced with KafkaTemplate.send(...)
    }
}
