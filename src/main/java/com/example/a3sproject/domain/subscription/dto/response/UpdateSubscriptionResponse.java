package com.example.a3sproject.domain.subscription.dto.response;

import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateSubscriptionResponse(
        boolean success,

        @JsonProperty("subscriptionId")
        String subscriptionUuid,
        SubscriptionStatus status
) {
}
//name: success
//type: boolean
//required: true
//description: 성공 여부
//            - name: subscriptionId
//type: string
//required: false
//        - name: status
//type: string
//required: false
//description: 변경된 구독 상태