package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.config.PortOneProperties;
import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.BillingKeyPaymentRequest;
import com.example.a3sproject.domain.portone.dto.BillingKeyPaymentResponse;
import com.example.a3sproject.domain.portone.dto.ValidateBillingKeyResponse;
import com.example.a3sproject.domain.subscription.dtos.request.CreateBillingRequest;
import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateBillingResponse;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetSubscriptionResponse;
import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.example.a3sproject.domain.subscription.repository.SubsciptionBillingRepository;
import com.example.a3sproject.domain.subscription.repository.SubsciptionRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.common.GenerateCodeUuid;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import com.example.a3sproject.global.exception.domain.SubscriptionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubsciptionService {
    private final SubsciptionRepository subsciptionRepository;
    private final SubsciptionBillingRepository subsciptionBillingRepository;
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
        // 소유권 검증
        if(!subscription.getUser().getId().equals(userId)) {
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

    // 수동 즉시 청구
    public CreateBillingResponse createBilling(Long userId, String subscriptionId, CreateBillingRequest request) {
        // 본인 구독인지 확인
        Subscription subscription = subsciptionRepository.findBySubscriptionUuidAndUserId(subscriptionId, userId).orElseThrow(
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
                    LocalDateTime.now(),
                    LocalDateTime.parse(request.getPeriodStart()),
                    LocalDateTime.parse(request.getPeriodEnd()),
                    null
            );
            SubscriptionBilling savedBilling = subsciptionBillingRepository.save(subscriptionBillingSuccess);

            return new CreateBillingResponse(
                    true,
                    savedBilling.getId().toString(),
                    paymentId,
                    amount,
                    SubscriptionBillingStatus.COMPLETED
            );
        }catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            // 실패시 구독 청구에 저장
            SubscriptionBilling subscriptionBillingFail = new SubscriptionBilling(
                    subscription,
                    amount,
                    SubscriptionBillingStatus.FAILED,
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.parse(request.getPeriodStart()),
                    LocalDateTime.parse(request.getPeriodEnd()),
                    e.getMessage()
            );
            subsciptionBillingRepository.save(subscriptionBillingFail);
            return new CreateBillingResponse(
                    false,
                    null,
                    null,
                    amount,
                    SubscriptionBillingStatus.FAILED
            );
        }
    }

}
