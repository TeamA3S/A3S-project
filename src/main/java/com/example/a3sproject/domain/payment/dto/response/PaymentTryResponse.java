package com.example.a3sproject.domain.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentTryResponse(
        boolean success,
        @JsonProperty("paymentId")
        String portOneId,
        String orderName,
        int actualAmount,
        String currency,
        String status
) {
}
