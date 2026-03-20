package com.example.a3sproject.domain.subscription.dtos.request;

public record CreateSubscriptionRequest(
        String customerUid,
        String planId,
        String billingKey,
        int amount
) {
}
//customerUid
//type: string
//required: true
//description: PortOne 고객 고유 식별자
//- name: planId
//type: string
//required: true
//description: 구독할 플랜 ID
//- name: billingKey
//type: string
//required: true
//description: PortOne에서 발급받은 빌링키 (서버에서 결제수단으로 저장)
//- name: amount
//type: number
//required: true
//description: 구독 금액
