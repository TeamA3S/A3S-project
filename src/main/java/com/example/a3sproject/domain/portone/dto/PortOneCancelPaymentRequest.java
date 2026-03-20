package com.example.a3sproject.domain.portone.dto;

public record PortOneCancelPaymentRequest(
        String reason,
        String storeId
) { }