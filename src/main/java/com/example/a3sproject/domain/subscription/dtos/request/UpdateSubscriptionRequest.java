package com.example.a3sproject.domain.subscription.dtos.request;

public record UpdateSubscriptionRequest(
        String action,
        String reason
) {
}
