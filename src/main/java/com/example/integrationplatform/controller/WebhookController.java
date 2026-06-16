package com.example.integrationplatform.controller;

import com.example.integrationplatform.dto.ApiResponse;
import com.example.integrationplatform.dto.WebhookRequest;
import com.example.integrationplatform.dto.WebhookResponse;
import com.example.integrationplatform.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/order")
    public ResponseEntity<ApiResponse<WebhookResponse>> receiveOrderWebhook(
            @Valid @RequestBody WebhookRequest request
    ) {
        WebhookResponse response = webhookService.receiveWebhook(request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Webhook received successfully", response));
    }
}