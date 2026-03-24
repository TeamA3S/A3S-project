package com.example.a3sproject.domain.subscription.dto.request;

public record UpdateSubscriptionRequest(
        String action,
        String reason
) {
}
