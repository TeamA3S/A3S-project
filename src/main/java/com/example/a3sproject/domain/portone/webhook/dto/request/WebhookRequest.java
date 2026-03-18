package com.example.a3sproject.domain.portone.webhook.dto.request;

public record WebhookRequest(
        String paymentId,
        String status
) {}