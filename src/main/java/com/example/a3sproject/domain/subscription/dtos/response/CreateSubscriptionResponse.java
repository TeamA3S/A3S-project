package com.example.a3sproject.domain.subscription.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateSubscriptionResponse(
//        @JsonProperty("subscriptionId")
        String subscriptionId
) {
}
//name: subscriptionId
//type: string
//required: true
//description: 생성된 구독 ID
//usage: 구독 관리 페이지 리다이렉트에 사용