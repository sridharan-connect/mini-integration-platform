package com.example.integrationplatform.service;

import com.example.integrationplatform.entity.WebhookEvent;

public interface EventPublisherService {

    void publish(WebhookEvent event);
}