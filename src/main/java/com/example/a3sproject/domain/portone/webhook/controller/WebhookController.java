package com.example.a3sproject.domain.portone.webhook.controller;

import com.example.a3sproject.domain.portone.webhook.dto.request.WebhookRequest;
import com.example.a3sproject.domain.portone.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/webhook")
public class WebhookController {
    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("webhook-id") String webhookId,
            @RequestBody WebhookRequest request
    ) {
        try {
            webhookService.handleWebhook(webhookId, request.paymentId(), request.status());
        } catch (Exception e) {
            log.error("웹훅 처리 실패 webhookId: {}, error: {}", webhookId, e.getMessage());
            // 실패해도 200 OK 반환! (PortOne 재전송 방지)
        }
        return ResponseEntity.ok().build();
    }
}