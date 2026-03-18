package com.example.a3sproject.domain.payment.dto.response;

public record PaymentConfirmResponse(
        String orderNumber,
        String message
) {}
