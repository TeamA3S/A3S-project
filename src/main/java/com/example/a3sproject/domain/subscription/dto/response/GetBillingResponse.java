package com.example.a3sproject.domain.subscription.dto.response;

import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record GetBillingResponse(
        String billingId, //얘만 좀 찾아보자
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        int amount,
        SubscriptionBillingStatus status,
        @Nullable String paymentId, //(선택)
        @Nullable LocalDateTime attemptDate, //(선택)
        @Nullable String failureMessage //(선택)
) {
}
