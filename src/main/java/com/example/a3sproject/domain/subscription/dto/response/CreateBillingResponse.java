package com.example.a3sproject.domain.subscription.dto.response;

import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import lombok.Getter;

@Getter
public class CreateBillingResponse {
    private final boolean success;
    private final String billingId;
    private final String paymentId;
    private final int amount;
    private final SubscriptionBillingStatus status;

    public CreateBillingResponse(boolean success, String billingId, String paymentId,
                                 int amount, SubscriptionBillingStatus status) {
        this.success = success;
        this.billingId = billingId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.status = status;
    }
}
