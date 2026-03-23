package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.ValidateBillingKeyResponse;
import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetSubscriptionResponse;
import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.example.a3sproject.domain.subscription.repository.SubsciptionBillingRepository;
import com.example.a3sproject.domain.subscription.repository.SubsciptionRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.SubscriptionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class SubsciptionService {
    private final SubsciptionRepository subsciptionRepository;
    private final SubsciptionBillingRepository subsciptionBillingRepository;
    private final PortOneClient portOneClient;
    private final SubscriptionTxService subscriptionTxService;
    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;



    public CreateSubscriptionResponse createSubscription(long userId, CreateSubscriptionRequest request) {
        // 1. PortOne API로 billingKey 유효성 검증
        ValidateBillingKeyResponse validateBillingKeyResponse = portOneClient.getBillingKey(request.billingKey());

        // 검증 추가
        if(!"ISSUED".equals(validateBillingKeyResponse.status())) {
            throw new SubscriptionException(ErrorCode.INVALID_BILLING_KEY);
        }

        if(subsciptionRepository.existsByUserIdAndPlanIdAndStatus(userId, request.planId(), SubscriptionStatus.ACTIVE)) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        return subscriptionTxService.saveSubscription(userId, request);
    }

    public GetSubscriptionResponse getSubscription(long userId, String subscriptionId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.USER_NOT_FOUND)
        );
        Subscription subscription = subsciptionRepository.findBySubscriptionUuid(subscriptionId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );
        if(!subscription.getUser().getId().equals(userId)) {
            throw new SubscriptionException(ErrorCode.USER_NOT_MATCH);
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findById(subscription.getPaymentMethodId()).orElseThrow(
                () -> new SubscriptionException(ErrorCode.PAYMENTMETHOD_NOT_FOUND)
        );
        return new GetSubscriptionResponse(
                subscription.getSubscriptionUuid(),
                user.getCustomerUid(),
                subscription.getPlan().getPlanUuid(), //추후 수정
                paymentMethod.getPaymentMethodUuid(), //추후 수정
                subscription.getStatus(),
                subscription.getAmount(),
                subscription.getCurrentPeriodEnd()
        );
    }

}
