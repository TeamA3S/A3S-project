package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.config.PortOneProperties;
import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.PortOneBillingKeyPaymentRequest;
import com.example.a3sproject.domain.portone.dto.PortOneBillingKeyPaymentResponse;
import com.example.a3sproject.domain.portone.dto.ValidateBillingKeyResponse;
import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;
import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetSubscriptionResponse;
import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.example.a3sproject.domain.subscription.repository.SubscriptionBillingRepository;
import com.example.a3sproject.domain.subscription.repository.SubscriptionRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.common.GenerateCodeUuid;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.SubscriptionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionBillingRepository subscriptionBillingRepository;
    private final PortOneClient portOneClient;
    private final SubscriptionTxService subscriptionTxService;
    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PortOneProperties portOneProperties;


    public CreateSubscriptionResponse createSubscription(long userId, CreateSubscriptionRequest request) {
        // 1. PortOne API로 billingKey 유효성 검증
        ValidateBillingKeyResponse validateBillingKeyResponse = portOneClient.getBillingKey(request.billingKey());

        // 검증 추가
        if(!"ISSUED".equals(validateBillingKeyResponse.status())) {
            throw new SubscriptionException(ErrorCode.INVALID_BILLING_KEY);
        }
        // 중복 검증
        if(subscriptionRepository.existsByUserIdAndPlanIdAndStatus(userId, request.planId(), SubscriptionStatus.ACTIVE)) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        return subscriptionTxService.saveSubscription(userId, request);
    }

    public GetSubscriptionResponse getSubscription(long userId, String subscriptionId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.USER_NOT_FOUND)
        );
        Subscription subscription = subscriptionRepository.findBySubscriptionUuid(subscriptionId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );
        // 소유권 검증
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

    @Transactional
    public void processScheduledBillings() {
        // 1. 결제 대상 구독 목록 조회
        List<Subscription> targets = subscriptionRepository
                .findByStatusInAndCurrentPeriodEndBefore(
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE),
                        OffsetDateTime.now()
                );

        for (Subscription subscription : targets) {
            // 2. billingKey 조회
            PaymentMethod paymentMethod = paymentMethodRepository
                    .findById(subscription.getPaymentMethodId())
                    .orElseThrow(() -> new SubscriptionException(ErrorCode.PAYMENTMETHOD_NOT_FOUND));

            // 3. paymentId 생성
            String paymentId = GenerateCodeUuid.generateCodeUuid("BIL");

            // 4. 요청 DTO 생성
            PortOneBillingKeyPaymentRequest request = new PortOneBillingKeyPaymentRequest(
                    portOneProperties.getStore().getId(),
                    paymentMethod.getBillingKey(),
                    subscription.getPlan().getName() + "플랜 정기결제",
                    new PortOneBillingKeyPaymentRequest.Amount(subscription.getAmount()),
                    new PortOneBillingKeyPaymentRequest.Customer(paymentMethod.getCustomerUid())
            );

            try {
                // 5. PortOne API 호출
                PortOneBillingKeyPaymentResponse response = portOneClient.payWithBillingKey(paymentId, request);

                // 6. 성공 시 처리
                if (PortOnePayStatus.PAID == response.status()) {
                    // 6-1. 구독 청구 이력 저장
                    SubscriptionBilling billing = SubscriptionBilling.builder()
                            .subscriptionUuid(subscription.getSubscriptionUuid())
                            .amount(subscription.getAmount())
                            .status(SubscriptionBillingStatus.COMPLETED)
                            .paymentId(paymentId)
                            .attemptDate(response.paidAt())
                            .periodStart(subscription.getCurrentPeriodEnd())
                            .periodEnd(subscription.getCurrentPeriodEnd().plusMonths(1))
                            .failureMessage(null)  // 성공이므로 null
                            .build();
                    subscriptionBillingRepository.save(billing);

                    // 6-2. 구독 기간 연장 및 상태 ACTIVE로 변경
                    subscription.renewPeriod();
                }

            } catch (Exception e) {
                log.error("정기결제 실패 subscriptionUuid: {}, error: {}",
                        subscription.getSubscriptionUuid(), e.getMessage());

                // 7. 실패 시 처리
                SubscriptionBilling billing = SubscriptionBilling.builder()
                        .subscriptionUuid(subscription.getSubscriptionUuid())
                        .amount(subscription.getAmount())
                        .status(SubscriptionBillingStatus.FAILED)
                        .paymentId(null)
                        .attemptDate(OffsetDateTime.now())
                        .periodStart(subscription.getCurrentPeriodEnd())
                        .periodEnd(subscription.getCurrentPeriodEnd().plusMonths(1))
                        .failureMessage(e.getMessage()) // 실패시 메시지
                        .build();
                subscriptionBillingRepository.save(billing);

                // 8. 구독 상태 PAST_DUE로 변경
                subscription.markAsPastDue();
            }
        }
    }

}
