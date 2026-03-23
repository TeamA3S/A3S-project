package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.paymentMethod.enums.PaymentMethodStatus;
import com.example.a3sproject.domain.paymentMethod.enums.PgProvider;
import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import com.example.a3sproject.domain.plan.entity.Plan;
import com.example.a3sproject.domain.plan.repository.PlanRepository;
import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.repository.SubsciptionRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.SubscriptionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 트랜잭션 사용을 위해서 분리
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionTxService {
    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final SubsciptionRepository subsciptionRepository;
    private final PlanRepository planRepository;

    public CreateSubscriptionResponse saveSubscription(long userId, CreateSubscriptionRequest request) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.USER_NOT_FOUND)
        );
        Plan plan = planRepository.findByPlanUuid(request.planId()).orElseThrow(
                () -> new SubscriptionException(ErrorCode.PLAN_NOT_FOUND)
        );

        // PaymentMethod 저장
        PaymentMethod paymentMethod = new PaymentMethod(
                user,
                request.billingKey(),
                request.customerUid(),
                PgProvider.TOSS_PAYMENTS, //Todo : 확인 필요
                true,
                PaymentMethodStatus.ACTIVE
        );
        paymentMethodRepository.save(paymentMethod);

        // Subscription 저장
        Subscription subscription = new Subscription(
                user,
                plan,
                paymentMethod.getId(),
                request.amount()
        );
        subsciptionRepository.save(subscription);

        return new CreateSubscriptionResponse(subscription.getSubscriptionUuid());
    }
}