package com.example.a3sproject.domain.portone.dto;

public record PortOneBillingKeyPaymentRequest(
        String storeId,
        String billingkey,
        String orderName,
        Amount amount,
        Customer customer
) {
    public record Amount(
            int total
    ) {}

    public record Customer(
            String customerId
    ) {}
}
