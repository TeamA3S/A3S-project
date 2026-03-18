package com.example.a3sproject.domain.payments.payment.dto.request;

public record PaymentConfirmRequest(
        long orderId,
        String portOneId

) {
}
