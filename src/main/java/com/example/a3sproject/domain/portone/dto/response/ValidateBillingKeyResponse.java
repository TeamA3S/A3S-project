package com.example.a3sproject.domain.portone.dto.response;

public record ValidateBillingKeyResponse(
        String status,
        String billingKey,
        String merchantId,
        String storeId
) {
}
