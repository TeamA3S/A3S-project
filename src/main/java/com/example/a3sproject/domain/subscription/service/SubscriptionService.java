package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.config.PortOneProperties;
import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import com.example.a3sproject.domain.plan.entity.Plan;
import com.example.a3sproject.domain.plan.repository.PlanRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.request.BillingKeyPaymentRequest;
import com.example.a3sproject.domain.portone.dto.response.BillingKeyPaymentResponse;
import com.example.a3sproject.domain.subscription.dto.request.CreateBillingRequest;
import com.example.a3sproject.domain.portone.dto.response.ValidateBillingKeyResponse;
import com.example.a3sproject.domain.subscription.dto.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dto.response.CreateBillingResponse;
import com.example.a3sproject.domain.subscription.dto.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.dto.response.GetAllBillingsResponse;
import com.example.a3sproject.domain.subscription.dto.response.GetBillingResponse;
import com.example.a3sproject.domain.subscription.dto.response.GetSubscriptionResponse;
import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.example.a3sproject.domain.subscription.repository.SubscriptionBillingRepository;
import com.example.a3sproject.domain.subscription.repository.SubscriptionRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.common.GenerateCodeUuid;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import com.example.a3sproject.global.exception.domain.SubscriptionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionBillingRepository subscriptionBillingRepository;
    private final PortOneClient portOneClient;
    private final SubscriptionTxService subscriptionTxService;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PortOneProperties portOneProperties;
    private final PlanRepository planRepository;


    public CreateSubscriptionResponse createSubscription(User user, CreateSubscriptionRequest request) {
        // 1. PortOne API로 billingKey 유효성 검증
        ValidateBillingKeyResponse validateBillingKeyResponse = portOneClient.getBillingKey(request.billingKey());

        if (!"ISSUED".equals(validateBillingKeyResponse.status())) {
            throw new SubscriptionException(ErrorCode.INVALID_BILLING_KEY);
        }

        Plan plan = planRepository.findByPlanUuid(request.planId()).orElseThrow(
                () -> new SubscriptionException(ErrorCode.PLAN_NOT_FOUND)
        );

        // 중복 검증
        if (subscriptionRepository.existsByUserAndPlanAndStatus(user, plan, SubscriptionStatus.ACTIVE)) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        CreateSubscriptionResponse response = subscriptionTxService.saveSubscription(user.getId(), request);

        // 즉시 결제 시도
        CreateBillingRequest firstBillingRequest = new CreateBillingRequest(
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMonths(1)
        );
        createBilling(user.getId(), response.subscriptionId(), firstBillingRequest);

        return response;
    }

    public GetSubscriptionResponse getSubscription(User user, String subscriptionId) {
        Subscription subscription = subscriptionRepository.findBySubscriptionUuid(subscriptionId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );

        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new SubscriptionException(ErrorCode.USER_NOT_MATCH);
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findById(subscription.getPaymentMethod().getId()).orElseThrow(
                () -> new SubscriptionException(ErrorCode.PAYMENTMETHOD_NOT_FOUND)
        );
        return new GetSubscriptionResponse(
                subscription.getSubscriptionUuid(),
                user.getCustomerUid(),
                subscription.getPlan().getPlanUuid(),
                paymentMethod.getPaymentMethodUuid(),
                subscription.getStatus(),
                subscription.getAmount(),
                subscription.getCurrentPeriodEnd()
        );
    }

    public GetAllBillingsResponse getBillings(User user, String subscriptionId) {
        Subscription subscription = subscriptionRepository.findBySubscriptionUuid(subscriptionId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );
        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new SubscriptionException(ErrorCode.USER_NOT_MATCH);
        }
        List<SubscriptionBilling> history = subscriptionBillingRepository.findBySubscription(subscription);
        List<GetBillingResponse> responses = new ArrayList<>();

        for (SubscriptionBilling billing : history) {
            GetBillingResponse response = new GetBillingResponse(
                    billing.getBillingHistoryUuid(),
                    billing.getPeriodStart(),
                    billing.getPeriodEnd(),
                    billing.getAmount(),
                    billing.getStatus(),
                    billing.getPaymentId(),
                    billing.getCreatedAt(),
                    billing.getFailureMessage()
            );
            responses.add(response);
        }

        return new GetAllBillingsResponse(responses);
    }

    @Transactional
    public void processScheduledBillings() {
        List<Subscription> targets = subscriptionRepository
                .findByStatusInAndCurrentPeriodEndBefore(
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE),
                        OffsetDateTime.now()
                );

        for (Subscription subscription : targets) {
            PaymentMethod paymentMethod = paymentMethodRepository
                    .findById(subscription.getPaymentMethod().getId())
                    .orElseThrow(() -> new SubscriptionException(ErrorCode.PAYMENTMETHOD_NOT_FOUND));

            String paymentId = GenerateCodeUuid.generateCodeUuid("SUB");

            BillingKeyPaymentRequest request = new BillingKeyPaymentRequest(
                    portOneProperties.getStore().getId(),
                    paymentMethod.getBillingKey(),
                    subscription.getPlan().getName() + " 정기결제",
                    new BillingKeyPaymentRequest.PaymentAmountInput(subscription.getAmount()),
                    "KRW"
            );

            try {
                BillingKeyPaymentResponse response = portOneClient.billingKeyPayment(paymentId, request);

                // 핵심: 명세서에 따른 status 필드 검증
                if (response != null && response.getPayment() != null) {
                    SubscriptionBilling billing = new SubscriptionBilling(
                            subscription,
                            subscription.getAmount(),
                            SubscriptionBillingStatus.PAID,
                            paymentId,
                            subscription.getCurrentPeriodEnd(),
                            subscription.getCurrentPeriodEnd().plusMonths(1),
                            null
                    );
                    subscriptionBillingRepository.save(billing);
                    subscription.renewPeriod();
                } else {
                    throw new SubscriptionException(ErrorCode.PAYMENT_PORTONE_ERROR);
                }

            } catch (Exception e) {
                log.error("정기결제 실패 subscription: {}, paymentId: {}, error: {}",
                        subscription.getSubscriptionUuid(), paymentId, e.getMessage());

                SubscriptionBilling billing = new SubscriptionBilling(
                        subscription,
                        subscription.getAmount(),
                        SubscriptionBillingStatus.FAILED,
                        paymentId, // NULL 대신 paymentId 저장
                        subscription.getCurrentPeriodEnd(),
                        subscription.getCurrentPeriodEnd().plusMonths(1),
                        e.getMessage()
                );
                subscriptionBillingRepository.save(billing);
                subscription.markAsPastDue();
            }
        }
    }

    public CreateBillingResponse createBilling(Long userId, String subscriptionId, CreateBillingRequest request) {
        Subscription subscription = subscriptionRepository.findBySubscriptionUuidAndUser_Id(subscriptionId, userId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );

        String billingKey = subscription.getPaymentMethod().getBillingKey();
        int amount = subscription.getPlan().getAmount();
        String paymentId = GenerateCodeUuid.generateCodeUuid("SUB");

        BillingKeyPaymentRequest billingKeyPaymentRequest = new BillingKeyPaymentRequest(
                portOneProperties.getStore().getId(),
                billingKey,
                subscription.getPlan().getName(),
                new BillingKeyPaymentRequest.PaymentAmountInput(amount),
                "KRW"
        );

        try {
            BillingKeyPaymentResponse response = portOneClient.billingKeyPayment(paymentId, billingKeyPaymentRequest);

            // paidAt 체크 대신 status가 PAID인지 명확히 확인
            if (response == null || response.getPayment() == null) {
                throw new SubscriptionException(ErrorCode.PAYMENT_PORTONE_ERROR);
            }

            SubscriptionBilling subscriptionBillingSuccess = new SubscriptionBilling(
                    subscription,
                    amount,
                    SubscriptionBillingStatus.PAID,
                    paymentId,
                    request.periodStart(),
                    request.periodEnd(),
                    null
            );
            SubscriptionBilling savedBilling = subscriptionBillingRepository.save(subscriptionBillingSuccess);

            return new CreateBillingResponse(
                    true,
                    savedBilling.getBillingHistoryUuid(),
                    paymentId,
                    amount,
                    SubscriptionBillingStatus.PAID
            );
        } catch (Exception e) {
            log.error("구독 즉시 결제 에러 - paymentId: {}, message: {}", paymentId, e.getMessage());
            SubscriptionBilling subscriptionBillingFail = new SubscriptionBilling(
                    subscription,
                    amount,
                    SubscriptionBillingStatus.FAILED,
                    paymentId, // paymentId 확실히 기록
                    request.periodStart(),
                    request.periodEnd(),
                    e.getMessage()
            );
            subscriptionBillingRepository.save(subscriptionBillingFail);
            return new CreateBillingResponse(false, null, null, amount, SubscriptionBillingStatus.FAILED);
        }
    }

    @Transactional
    public void cancelSubscription(long userId, String subscriptionId) {
        Subscription subscription = subscriptionRepository
                .findBySubscriptionUuid(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getUser().getId().equals(userId)) {
            throw new SubscriptionException(ErrorCode.USER_NOT_MATCH);
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED
                || subscription.getStatus() == SubscriptionStatus.ENDED) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED);
        }

        subscription.cancel();
    }
}
