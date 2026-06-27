package com.example.integrationplatform.service;

import com.example.integrationplatform.entity.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Profile("!mock-publisher")
public class KafkaEventPublisherService implements EventPublisherService {

    private static final Logger logger =
            LoggerFactory.getLogger(KafkaEventPublisherService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String webhookEventsTopic;

    public KafkaEventPublisherService(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.webhook-events}") String webhookEventsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.webhookEventsTopic = webhookEventsTopic;
    }

    @Override
    public void publish(WebhookEvent event) {
        try {
            String key = event.getSource() + ":" + event.getEventId();
            String message = buildMessage(event);

            var result = kafkaTemplate
                    .send(webhookEventsTopic, key, message)
                    .get(10, TimeUnit.SECONDS);

            var metadata = result.getRecordMetadata();

            logger.info(
                    "Webhook event published to Kafka. topic={}, partition={}, offset={}, key={}, eventId={}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    key,
                    event.getEventId()
            );

            logger.info(
                    "Webhook event published to Kafka. topic={}, key={}, eventId={}",
                    webhookEventsTopic,
                    key,
                    event.getEventId()
            );

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed to publish webhook event to Kafka. eventId=" + event.getEventId(),
                    ex
            );
        }
    }

    private String buildMessage(WebhookEvent event) throws Exception {
        Map<String, Object> message = new LinkedHashMap<>();

        message.put("id", event.getId());
        message.put("eventId", event.getEventId());
        message.put("source", event.getSource());
        message.put("eventType", event.getEventType());
        message.put("payload", event.getPayload());
        message.put("createdAt", event.getCreatedAt());

        return objectMapper.writeValueAsString(message);
    }
}