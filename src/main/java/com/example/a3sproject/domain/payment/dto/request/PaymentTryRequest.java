package com.example.a3sproject.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentTryRequest(
        @NotNull
        long orderId,
        @NotNull @Positive
        Integer totalAmount,
        Integer pointsToUse
) {
}