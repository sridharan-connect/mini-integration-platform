package com.example.integrationplatform.controller;

import com.example.integrationplatform.dto.RetryWebhookEventResponse;
import com.example.integrationplatform.service.WebhookEventRetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhook-events")
public class WebhookEventAdminController {

    private final WebhookEventRetryService webhookEventRetryService;

    public WebhookEventAdminController(WebhookEventRetryService webhookEventRetryService) {
        this.webhookEventRetryService = webhookEventRetryService;
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<RetryWebhookEventResponse> retryWebhookEvent(@PathVariable Long id) {
        RetryWebhookEventResponse response = webhookEventRetryService.retryEvent(id);
        return ResponseEntity.ok(response);
    }
}