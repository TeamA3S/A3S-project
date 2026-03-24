package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.config.PortOneProperties;
import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.request.BillingKeyPaymentRequest;
import com.example.a3sproject.domain.portone.dto.response.BillingKeyPaymentResponse;
import com.example.a3sproject.domain.subscription.dtos.request.CreateBillingRequest;
import com.example.a3sproject.domain.portone.dto.response.ValidateBillingKeyResponse;
import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateBillingResponse;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetAllBillingsResponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetBillingResponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetSubscriptionResponse;
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


    public CreateSubscriptionResponse createSubscription(long userId, CreateSubscriptionRequest request) {
        // 1. PortOne API로 billingKey 유효성 검증
        ValidateBillingKeyResponse validateBillingKeyResponse = portOneClient.getBillingKey(request.billingKey());

        // 검증 추가
        if (!"ISSUED".equals(validateBillingKeyResponse.status())) {
            throw new SubscriptionException(ErrorCode.INVALID_BILLING_KEY);
        }
        // 중복 검증
        if (subscriptionRepository.existsByUserIdAndPlanIdAndStatus(userId, request.planId(), SubscriptionStatus.ACTIVE)) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        CreateSubscriptionResponse response = subscriptionTxService.saveSubscription(userId, request);

        // 즉시 결제
        CreateBillingRequest firstBillingRequest = new CreateBillingRequest(
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMonths(1)
        );
        createBilling(userId, response.subscriptionUuid(), firstBillingRequest);

        return response;
    }

    public GetSubscriptionResponse getSubscription(User user, String subscriptionId) {
        Subscription subscription = subscriptionRepository.findBySubscriptionUuid(subscriptionId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );
        // 소유권 검증

        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new SubscriptionException(ErrorCode.USER_NOT_MATCH);
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findById(subscription.getPaymentMethod().getId()).orElseThrow(
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

    public GetAllBillingsResponse getBillings(User user, String subscriptionId) {
        Subscription subscription = subscriptionRepository.findBySubscriptionUuid(subscriptionId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );
        // 소유권 검증
        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new SubscriptionException(ErrorCode.USER_NOT_MATCH);
        }
        List<SubscriptionBilling> history = subscriptionBillingRepository.findBySubscription(subscription);
        List<GetBillingResponse> responses = new ArrayList<>();

        for (SubscriptionBilling billing : history) {
            GetBillingResponse response = new GetBillingResponse(
                    billing.getBillingUuid(),
                    billing.getPeriodStart(),
                    billing.getPeriodEnd(),
                    billing.getAmount(),
                    billing.getStatus(),
                    billing.getPaymentId(), //(선택)
                    billing.getCreatedAt(), //(선택)
                    billing.getFailureMessage() //(선택)
            );
            responses.add(response);
        }

        return new GetAllBillingsResponse(responses);
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
                    .findById(subscription.getPaymentMethod().getId())
                    .orElseThrow(() -> new SubscriptionException(ErrorCode.PAYMENTMETHOD_NOT_FOUND));

            // 3. paymentId 생성
            String paymentId = GenerateCodeUuid.generateCodeUuid("SUB");

            // 4. 요청 DTO 생성
            BillingKeyPaymentRequest request = new BillingKeyPaymentRequest(
                    portOneProperties.getStore().getId(),
                    paymentMethod.getBillingKey(),
                    subscription.getPlan().getName() + "플랜 정기결제",
                    new BillingKeyPaymentRequest.PaymentAmountInput(subscription.getAmount()),
                    "KRW"
            );

            try {
                // 5. PortOne API 호출
                BillingKeyPaymentResponse response = portOneClient.billingKeyPayment(paymentId, request);

                // 6. 성공 시 처리
                if ("PAID".equals(response.getStatus())) {
                    // 6-1. 구독 청구 이력 저장
                    SubscriptionBilling billing = new SubscriptionBilling(
                            subscription,
                            subscription.getAmount(),
                            SubscriptionBillingStatus.COMPLETED,
                            paymentId,
//                            response.getPaidAt(),
                            subscription.getCurrentPeriodEnd(),
                            subscription.getCurrentPeriodEnd().plusMonths(1),
                            null
                    );
                    subscriptionBillingRepository.save(billing);

                    // 6-2. 구독 기간 연장 및 상태 ACTIVE로 변경
                    subscription.renewPeriod();
                }

            } catch (Exception e) {
                log.error("정기결제 실패 subscriptionUuid: {}, error: {}",
                        subscription.getSubscriptionUuid(), e.getMessage());

                // 7. 실패 시 처리
                SubscriptionBilling billing = new SubscriptionBilling(
                        subscription,
                        subscription.getAmount(),
                        SubscriptionBillingStatus.FAILED,
                        null,
//                        OffsetDateTime.now(),
                        subscription.getCurrentPeriodEnd(),
                        subscription.getCurrentPeriodEnd().plusMonths(1),
                        e.getMessage()
                );
                subscriptionBillingRepository.save(billing);

                // 8. 구독 상태 PAST_DUE로 변경
                subscription.markAsPastDue();
            }
        }

    }

    // 수동 즉시 청구
    public CreateBillingResponse createBilling(Long userId, String subscriptionId, CreateBillingRequest request) {
        // 본인 구독인지 확인
        Subscription subscription = subscriptionRepository.findBySubscriptionUuidAndUser_Id(subscriptionId, userId).orElseThrow(
                () -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );
        // 빌링키 가져오기
        String billingKey = subscription.getPaymentMethod().getBillingKey();

        // 플랜 금액 가져오기;
        int amount = subscription.getPlan().getAmount();

        // 포트원에 빌링키 결제 API 호출
        String paymentId = GenerateCodeUuid.generateCodeUuid("SUB");
        BillingKeyPaymentRequest billingKeyPaymentRequest = new BillingKeyPaymentRequest(
                portOneProperties.getStore().getId(),
                billingKey,
                subscription.getPlan().getName(),
                new BillingKeyPaymentRequest.PaymentAmountInput(amount),
                "KRW"
        );
        try {
            // 결제 시도
            BillingKeyPaymentResponse response = portOneClient.billingKeyPayment(paymentId, billingKeyPaymentRequest);

            if (!"PAID".equals(response.getStatus())) {
                throw new SubscriptionException(ErrorCode.PAYMENT_PORTONE_ERROR);
            }
            // 성공 시 구독 청구에 저장
            SubscriptionBilling subscriptionBillingSuccess = new SubscriptionBilling(
                    subscription,
                    amount,
                    SubscriptionBillingStatus.COMPLETED,
                    paymentId,
                    request.periodStart(),
                    request.periodEnd(),
                    null
            );
            SubscriptionBilling savedBilling = subscriptionBillingRepository.save(subscriptionBillingSuccess);

            return new CreateBillingResponse(
                    true,
                    savedBilling.getId().toString(),
                    paymentId,
                    amount,
                    SubscriptionBillingStatus.COMPLETED
            );
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            // 실패시 구독 청구에 저장
            SubscriptionBilling subscriptionBillingFail = new SubscriptionBilling(
                    subscription,
                    amount,
                    SubscriptionBillingStatus.FAILED,
                    null,
                    request.periodStart(),
                    request.periodEnd(),
                    e.getMessage()
            );
            subscriptionBillingRepository.save(subscriptionBillingFail);
            return new CreateBillingResponse(
                    false,
                    null,
                    null,
                    amount,
                    SubscriptionBillingStatus.FAILED
            );
        }
    }

    @Transactional
    public void cancelSubscription(long userId, String subscriptionId) {
        // 1. 구독 조회
        Subscription subscription = subscriptionRepository
                .findBySubscriptionUuid(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 2. 소유권 조회
        if (!subscription.getUser().getId().equals(userId)) {
            throw new SubscriptionException(ErrorCode.USER_NOT_MATCH);
        }

        // 3. 이미 해지/종료된 구독인지 확인
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED
                || subscription.getStatus() == SubscriptionStatus.ENDED) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED);
        }

        // 4. 구독 해지 (canceledAt 자동 저장, Dirty Checking으로 save() 불필요)
        subscription.cancel();
    }

}
