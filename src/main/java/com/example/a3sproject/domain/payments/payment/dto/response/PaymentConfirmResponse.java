package com.example.a3sproject.domain.payments.payment.dto.response;

public record PaymentConfirmResponse(
        String orderNumber,
        String message
) {}
