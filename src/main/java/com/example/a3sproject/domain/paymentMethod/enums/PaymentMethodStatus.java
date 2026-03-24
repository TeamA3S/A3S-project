package com.example.a3sproject.domain.paymentMethod.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethodStatus {
    ACTIVE("활성"),
    INACTIVE("비활성"),
    DELETED("삭제");

    private final String pmStatus;
}
