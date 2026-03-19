package com.example.a3sproject.domain.payment.dto.response;

public record PaymentTryResponse(
        boolean success,
        String portOneId,
        String orderName,
        int actualAmount,
        String currency,
        String status
) {
}
