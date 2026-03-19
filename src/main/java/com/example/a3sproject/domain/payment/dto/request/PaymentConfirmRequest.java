package com.example.a3sproject.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentConfirmRequest(
        @NotNull
        long orderId,
        String portOneId,
        @NotNull @Positive
        Integer totalAmount,
        Integer pointsToUse
) {
        public int pointsToUseOrZero() {
                return pointsToUse != null ? pointsToUse : 0;
        }
}
