package com.example.a3sproject.domain.payment.dto.request;

public record PaymentConfirmRequest(
        long orderId,
        String portOneId

) {
}
