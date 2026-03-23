package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.ValidateBillingKeyResponse;
import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.repository.SubsciptionBillingRepository;
import com.example.a3sproject.domain.subscription.repository.SubsciptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubsciptionService {
    private final SubsciptionRepository subsciptionRepository;
    private final SubsciptionBillingRepository subsciptionBillingRepository;
    private final PortOneClient portOneClient;
//    private final SubscriptionTxService subscriptionTxService;


//    public CreateSubscriptionResponse createSubscription(long userId, CreateSubscriptionRequest request) {
//        // 1. PortOne API로 billingKey 유효성 검증
//        ValidateBillingKeyResponse validateBillingKeyResponse = portOneClient.getBillingKey(request.billingKey());
//
//        return subscriptionTxService.saveSubscription(userId, request);
//        // 2. PaymentMethod 저장
//
//        // 3. Subscription 생성
//
//    }

}
