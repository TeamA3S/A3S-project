package com.example.a3sproject.domain.portone.dto;

import lombok.Getter;

@Getter
public class BillingKeyPaymentRequest {

    private String storeId;
    private String billingKey;
    private String orderName;
    private PaymentAmountInput amount;
    private String currency;

    @Getter
    public static class PaymentAmountInput {
        private final int total;

        public PaymentAmountInput(int total) {
            this.total = total;
        }
    }

    public BillingKeyPaymentRequest(String storeId, String billingKey,
                                    String orderName, PaymentAmountInput amount, String currency) {
        this.storeId = storeId;
        this.billingKey = billingKey;
        this.orderName = orderName;
        this.amount = amount;
        this.currency = currency;
    }
}
