package com.example.a3sproject.domain.subscription.dtos.response;

import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record GetSubscriptionResponse(
        @JsonProperty("subscriptionId")
        String subscriptionUuid,

        String customerUid,

        @JsonProperty("planId")
        String planUuid,

        String paymentMethodId,
        SubscriptionStatus status,
        int amount,
        OffsetDateTime currentPeriodEnd
) {
}
//name: subscriptionId
//type: string
//required: true
//- name: customerUid
//type: string
//required: true
//description: PortOne 고객 고유 식별자
//- name: planId
//type: string
//required: true
//- name: paymentMethodId
//type: string
//required: false
//description: 결제 수단 ID (결제수단 테이블 참조)
//- name: status
//type: string
//required: true
//description: "구독 상태 (ACTIVE: 활성, CANCELLED: 해지, SUSPENDED: 미납, EXPIRED: 기간 종료)"
//- name: amount
//type: number
//required: true
//- name: currentPeriodEnd
//type: string
//required: true
//description: "현재 이용 기간 종료일 (ISO 8601 형식, 예: 2026-03-01T23:59:59)"