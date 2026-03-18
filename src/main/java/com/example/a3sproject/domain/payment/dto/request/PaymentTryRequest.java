package com.example.a3sproject.domain.payment.dto.request;

public record PaymentTryRequest(
        long orderId,
        int paidAmount,
        Integer pointsToUse
) {
}