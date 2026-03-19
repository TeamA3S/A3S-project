package com.example.a3sproject.domain.payment.dto;

// 처리 결과를 담는 record
public record PaymentProcessResult(
        boolean portOneConfirmed,
        String portOneId
) {
}
