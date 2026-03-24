package com.example.a3sproject.domain.portone.dto.response;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class BillingKeyPaymentResponse {
    private PaymentDetails payment;

    @Getter
    public static class PaymentDetails {
        private String id;
        private String status;
        private String paidAt; // ISO-8601 문자열 또는 OffsetDateTime
    }
}
