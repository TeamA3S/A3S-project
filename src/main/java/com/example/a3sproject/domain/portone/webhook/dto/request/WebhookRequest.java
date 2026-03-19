package com.example.a3sproject.domain.portone.webhook.dto.request;

public record WebhookRequest(
        String portOneId,
        String status
) {}