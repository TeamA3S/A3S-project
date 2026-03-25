package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.config.PortOneProperties;
import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import com.example.a3sproject.domain.plan.entity.Plan;
import com.example.a3sproject.domain.plan.repository.PlanRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.request.BillingKeyPaymentRequest;
import com.example.a3sproject.domain.portone.dto.response.BillingKeyPaymentResponse;
import com.example.a3sproject.domain.portone.dto.response.ValidateBillingKeyResponse;
import com.example.a3sproject.domain.subscription.dto.request.CreateBillingRequest;
import com.example.a3sproject.domain.subscription.dto.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dto.response.CreateBillingResponse;
import com.example.a3sproject.domain.subscription.dto.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.example.a3sproject.domain.subscription.repository.SubscriptionBillingRepository;
import com.example.a3sproject.domain.subscription.repository.SubscriptionRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.SubscriptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionBillingRepository subscriptionBillingRepository;
    @Mock private PortOneClient portOneClient;
    @Mock private SubscriptionTxService subscriptionTxService;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private PortOneProperties portOneProperties;
    @Mock private PlanRepository planRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    // ===================== createSubscription =====================

    @Test
    @DisplayName("구독 생성 - 유효한 billingKey와 플랜으로 구독 및 즉시 결제가 정상 생성된다")
    void createSubscription_정상요청_구독생성성공() {
        // given: 유효한 빌링키, 활성 플랜, 중복 구독 없음, 즉시결제 성공
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        Plan plan = mock(Plan.class);
        given(plan.getAmount()).willReturn(9900);
        given(plan.getName()).willReturn("프로 플랜");

        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        given(paymentMethod.getBillingKey()).willReturn("billing-key-001");

        Subscription subscription = mock(Subscription.class);
        given(subscription.getPaymentMethod()).willReturn(paymentMethod);
        given(subscription.getPlan()).willReturn(plan);

        CreateSubscriptionRequest request = new CreateSubscriptionRequest("CUST-1", "plan-uuid-001", "billing-key-001", 9900);

        ValidateBillingKeyResponse validResponse = mock(ValidateBillingKeyResponse.class);
        given(validResponse.status()).willReturn("ISSUED");
        given(portOneClient.getBillingKey("billing-key-001")).willReturn(validResponse);

        given(planRepository.findByPlanUuid("plan-uuid-001")).willReturn(Optional.of(plan));
        given(subscriptionRepository.existsByUserAndPlanAndStatus(user, plan, SubscriptionStatus.ACTIVE)).willReturn(false);
        given(subscriptionTxService.saveSubscription(1L, request)).willReturn(new CreateSubscriptionResponse("sub-uuid-001"));

        // createBilling 내부에서 사용하는 의존성 설정
        given(subscriptionRepository.findBySubscriptionUuidAndUser_Id("sub-uuid-001", 1L)).willReturn(Optional.of(subscription));

        PortOneProperties.Store store = mock(PortOneProperties.Store.class);
        given(store.getId()).willReturn("store-001");
        given(portOneProperties.getStore()).willReturn(store);

        BillingKeyPaymentResponse.PaymentDetails payment = mock(BillingKeyPaymentResponse.PaymentDetails.class);
        given(payment.getPaidAt()).willReturn(String.valueOf(OffsetDateTime.now()));
        BillingKeyPaymentResponse billingResponse = mock(BillingKeyPaymentResponse.class);
        given(billingResponse.getPayment()).willReturn(payment);
        given(portOneClient.billingKeyPayment(anyString(), any())).willReturn(billingResponse);

        SubscriptionBilling savedBilling = mock(SubscriptionBilling.class);
        given(savedBilling.getBillingHistoryUuid()).willReturn("billing-uuid-001");
        given(subscriptionBillingRepository.save(any())).willReturn(savedBilling);

        // when
        CreateSubscriptionResponse response = subscriptionService.createSubscription(user, request);

        // then: 구독 UUID 반환, 즉시결제 API 1회 호출
        assertThat(response.subscriptionId()).isEqualTo("sub-uuid-001");
        verify(portOneClient, times(1)).billingKeyPayment(anyString(), any(BillingKeyPaymentRequest.class));
    }

    @Test
    @DisplayName("구독 생성 - 유효하지 않은 billingKey 상태면 SubscriptionException이 발생한다")
    void createSubscription_유효하지않은빌링키_예외발생() {
        // given: billingKey 상태가 ISSUED가 아닌 경우
        User user = mock(User.class);
        // customerUid, planId, billingKey 순 — billingKey 자리에 "invalid-key"
        CreateSubscriptionRequest request = new CreateSubscriptionRequest("CUST-1", "plan-uuid-001", "invalid-key", 9900);

        ValidateBillingKeyResponse invalidResponse = mock(ValidateBillingKeyResponse.class);
        given(invalidResponse.status()).willReturn("DELETED");
        given(portOneClient.getBillingKey("invalid-key")).willReturn(invalidResponse);

        // when & then
        assertThatThrownBy(() -> subscriptionService.createSubscription(user, request))
                .isInstanceOf(SubscriptionException.class)
                .satisfies(e -> assertThat(((SubscriptionException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_BILLING_KEY));
    }

    @Test
    @DisplayName("구독 생성 - 동일 플랜으로 이미 활성 구독이 존재하면 SubscriptionException이 발생한다")
    void createSubscription_중복활성구독_예외발생() {
        // given: billingKey 유효, 플랜 존재, 중복 구독 존재
        User user = mock(User.class);
        Plan plan = mock(Plan.class);
        CreateSubscriptionRequest request = new CreateSubscriptionRequest("CUST-1", "plan-uuid-001", "billing-key-001", 9900);

        ValidateBillingKeyResponse validResponse = mock(ValidateBillingKeyResponse.class);
        given(validResponse.status()).willReturn("ISSUED");
        given(portOneClient.getBillingKey("billing-key-001")).willReturn(validResponse);
        given(planRepository.findByPlanUuid("plan-uuid-001")).willReturn(Optional.of(plan));
        given(subscriptionRepository.existsByUserAndPlanAndStatus(user, plan, SubscriptionStatus.ACTIVE)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> subscriptionService.createSubscription(user, request))
                .isInstanceOf(SubscriptionException.class)
                .satisfies(e -> assertThat(((SubscriptionException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS));
    }

    // ===================== getSubscription =====================

    @Test
    @DisplayName("구독 조회 - 본인 구독이면 구독 상세 정보를 반환한다")
    void getSubscription_본인구독_정상조회() {
        // given: 구독 존재, 소유권 일치, 결제 수단 존재
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        User subscriptionUser = mock(User.class);
        given(subscriptionUser.getId()).willReturn(1L);

        Plan plan = mock(Plan.class);
        given(plan.getPlanUuid()).willReturn("plan-uuid-001");

        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        given(paymentMethod.getId()).willReturn(10L);
        given(paymentMethod.getPaymentMethodUuid()).willReturn("pm-uuid-001");

        Subscription subscription = mock(Subscription.class);
        given(subscription.getUser()).willReturn(subscriptionUser);
        given(subscription.getPlan()).willReturn(plan);
        given(subscription.getPaymentMethod()).willReturn(paymentMethod);
        given(subscription.getSubscriptionUuid()).willReturn("sub-uuid-001");
        given(subscription.getStatus()).willReturn(SubscriptionStatus.ACTIVE);
        given(subscription.getAmount()).willReturn(9900);
        given(subscription.getCurrentPeriodEnd()).willReturn(OffsetDateTime.now().plusMonths(1));

        given(subscriptionRepository.findBySubscriptionUuid("sub-uuid-001")).willReturn(Optional.of(subscription));
        given(paymentMethodRepository.findById(10L)).willReturn(Optional.of(paymentMethod));

        // when
        var response = subscriptionService.getSubscription(user, "sub-uuid-001");

        // then
        assertThat(response.subscriptionUuid()).isEqualTo("sub-uuid-001");
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.amount()).isEqualTo(9900);
    }

    @Test
    @DisplayName("구독 조회 - 타인의 구독을 조회하면 SubscriptionException이 발생한다")
    void getSubscription_타인구독조회_예외발생() {
        // given: 구독 소유자와 요청 사용자 ID가 다름
        User requestUser = mock(User.class);
        given(requestUser.getId()).willReturn(2L);

        User owner = mock(User.class);
        given(owner.getId()).willReturn(1L);

        Subscription subscription = mock(Subscription.class);
        given(subscription.getUser()).willReturn(owner);
        given(subscriptionRepository.findBySubscriptionUuid("sub-uuid-001")).willReturn(Optional.of(subscription));

        // when & then
        assertThatThrownBy(() -> subscriptionService.getSubscription(requestUser, "sub-uuid-001"))
                .isInstanceOf(SubscriptionException.class)
                .satisfies(e -> assertThat(((SubscriptionException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_MATCH));
    }

    // ===================== cancelSubscription =====================

    @Test
    @DisplayName("구독 해지 - 활성 구독을 본인이 해지하면 cancel()이 호출된다")
    void cancelSubscription_활성구독해지_성공() {
        // given: 활성 구독, 소유권 일치
        User owner = mock(User.class);
        given(owner.getId()).willReturn(1L);

        Subscription subscription = mock(Subscription.class);
        given(subscription.getUser()).willReturn(owner);
        given(subscription.getStatus()).willReturn(SubscriptionStatus.ACTIVE);
        given(subscriptionRepository.findBySubscriptionUuid("sub-uuid-001")).willReturn(Optional.of(subscription));

        // when
        subscriptionService.cancelSubscription(1L, "sub-uuid-001");

        // then: 해지 메서드 1회 호출 검증
        verify(subscription, times(1)).cancel();
    }

    @Test
    @DisplayName("구독 해지 - 이미 해지된 구독을 다시 해지하면 SubscriptionException이 발생한다")
    void cancelSubscription_이미해지된구독_예외발생() {
        // given: 구독 상태가 이미 CANCELLED
        User owner = mock(User.class);
        given(owner.getId()).willReturn(1L);

        Subscription subscription = mock(Subscription.class);
        given(subscription.getUser()).willReturn(owner);
        given(subscription.getStatus()).willReturn(SubscriptionStatus.CANCELLED);
        given(subscriptionRepository.findBySubscriptionUuid("sub-uuid-001")).willReturn(Optional.of(subscription));

        // when & then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L, "sub-uuid-001"))
                .isInstanceOf(SubscriptionException.class)
                .satisfies(e -> assertThat(((SubscriptionException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED));
    }

    @Test
    @DisplayName("구독 해지 - 종료된 구독을 해지 요청하면 SubscriptionException이 발생한다")
    void cancelSubscription_종료된구독_예외발생() {
        // given: 구독 상태가 ENDED
        User owner = mock(User.class);
        given(owner.getId()).willReturn(1L);

        Subscription subscription = mock(Subscription.class);
        given(subscription.getUser()).willReturn(owner);
        given(subscription.getStatus()).willReturn(SubscriptionStatus.ENDED);
        given(subscriptionRepository.findBySubscriptionUuid("sub-uuid-001")).willReturn(Optional.of(subscription));

        // when & then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L, "sub-uuid-001"))
                .isInstanceOf(SubscriptionException.class)
                .satisfies(e -> assertThat(((SubscriptionException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED));
    }

    @Test
    @DisplayName("구독 해지 - 타인의 구독을 해지하려 하면 SubscriptionException이 발생한다")
    void cancelSubscription_타인구독해지시도_예외발생() {
        // given: 요청자와 구독 소유자가 다름
        User owner = mock(User.class);
        given(owner.getId()).willReturn(1L);

        Subscription subscription = mock(Subscription.class);
        given(subscription.getUser()).willReturn(owner);
        given(subscriptionRepository.findBySubscriptionUuid("sub-uuid-001")).willReturn(Optional.of(subscription));

        // when & then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(2L, "sub-uuid-001"))
                .isInstanceOf(SubscriptionException.class)
                .satisfies(e -> assertThat(((SubscriptionException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_MATCH));
    }

    // ===================== processScheduledBillings =====================

    @Test
    @DisplayName("정기결제 스케줄러 - 결제 성공 시 PAID 상태의 빌링이 저장되고 구독 기간이 갱신된다")
    void processScheduledBillings_결제성공_빌링저장및기간갱신() {
        // given: ACTIVE 구독 1건, 결제 성공 응답
        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        lenient().when(paymentMethod.getId()).thenReturn(1L);
        lenient().when(paymentMethod.getBillingKey()).thenReturn("billing-key-001");

        Plan plan = mock(Plan.class);
        lenient().when(plan.getName()).thenReturn("프로 플랜");

        Subscription subscription = mock(Subscription.class);
        lenient().when(subscription.getPaymentMethod()).thenReturn(paymentMethod);
        lenient().when(subscription.getPlan()).thenReturn(plan);
        lenient().when(subscription.getAmount()).thenReturn(9900);
        lenient().when(subscription.getCurrentPeriodEnd()).thenReturn(OffsetDateTime.now().minusDays(1));
        lenient().when(subscription.getSubscriptionUuid()).thenReturn("sub-uuid-001");

        lenient().when(subscriptionRepository.findByStatusInAndCurrentPeriodEndBefore(any(), any()))
                .thenReturn(List.of(subscription));
        lenient().when(paymentMethodRepository.findById(any())).thenReturn(Optional.of(paymentMethod));

        PortOneProperties.Store store = mock(PortOneProperties.Store.class);
        lenient().when(store.getId()).thenReturn("store-001");
        lenient().when(portOneProperties.getStore()).thenReturn(store);

        BillingKeyPaymentResponse.PaymentDetails payment = mock(BillingKeyPaymentResponse.PaymentDetails.class);
        lenient().when(payment.getPaidAt()).thenReturn("2026-03-25T16:41:33+09:00");
        BillingKeyPaymentResponse billingResponse = mock(BillingKeyPaymentResponse.class);
        lenient().when(billingResponse.getPayment()).thenReturn(payment);
        lenient().when(portOneClient.billingKeyPayment(anyString(), any())).thenReturn(billingResponse);

        // when
        subscriptionService.processScheduledBillings();

        // then: PAID 빌링 저장 1회, renewPeriod 1회 호출
        verify(subscriptionBillingRepository, times(1)).save(any());
        verify(subscription, times(1)).renewPeriod();
    }

    @Test
    @DisplayName("정기결제 스케줄러 - 결제 실패 시 FAILED 상태의 빌링이 저장되고 구독이 PAST_DUE로 변경된다")
    void processScheduledBillings_결제실패_PAST_DUE처리() {
        // given: ACTIVE 구독 1건, PortOne 결제 API 예외 발생
        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        given(paymentMethod.getBillingKey()).willReturn("billing-key-001");

        Plan plan = mock(Plan.class);
        given(plan.getName()).willReturn("프로 플랜");

        Subscription subscription = mock(Subscription.class);
        given(subscription.getPaymentMethod()).willReturn(paymentMethod);
        given(subscription.getPlan()).willReturn(plan);
        given(subscription.getAmount()).willReturn(9900);
        given(subscription.getCurrentPeriodEnd()).willReturn(OffsetDateTime.now().minusDays(1));
        given(subscription.getSubscriptionUuid()).willReturn("sub-uuid-001");

        given(subscriptionRepository.findByStatusInAndCurrentPeriodEndBefore(anyList(), any()))
                .willReturn(List.of(subscription));
        given(paymentMethodRepository.findById(any())).willReturn(Optional.of(paymentMethod));

        PortOneProperties.Store store = mock(PortOneProperties.Store.class);
        given(store.getId()).willReturn("store-001");
        given(portOneProperties.getStore()).willReturn(store);

        // PortOne 결제 API 호출 시 예외 발생
        given(portOneClient.billingKeyPayment(anyString(), any())).willThrow(new RuntimeException("카드 한도 초과"));

        SubscriptionBilling savedBilling = mock(SubscriptionBilling.class);
        given(subscriptionBillingRepository.save(any())).willReturn(savedBilling);

        // when
        subscriptionService.processScheduledBillings();

        // then: FAILED 빌링 저장 1회, markAsPastDue 1회 호출, renewPeriod 미호출
        verify(subscriptionBillingRepository, times(1)).save(argThat(billing ->
                billing.getStatus() == SubscriptionBillingStatus.FAILED
        ));
        verify(subscription, times(1)).markAsPastDue();
        verify(subscription, never()).renewPeriod();
    }

    @Test
    @DisplayName("정기결제 스케줄러 - 대상 구독이 없으면 아무 처리도 하지 않는다")
    void processScheduledBillings_대상없음_아무처리안함() {
        // given: 결제 대상 구독 없음
        given(subscriptionRepository.findByStatusInAndCurrentPeriodEndBefore(anyList(), any()))
                .willReturn(List.of());

        // when
        subscriptionService.processScheduledBillings();

        // then: 결제 API 및 빌링 저장 미호출
        verify(portOneClient, never()).billingKeyPayment(anyString(), any());
        verify(subscriptionBillingRepository, never()).save(any());
    }

    // ===================== createBilling =====================

    @Test
    @DisplayName("수동 즉시 청구 - paidAt이 null이면 PENDING 상태의 빌링이 저장된다")
    void createBilling_paidAt없음_PENDING저장() {
        // given: 구독 존재, PortOne 응답에 paidAt 없음(웹훅 후속 통지 케이스)
        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        given(paymentMethod.getBillingKey()).willReturn("billing-key-001");

        Plan plan = mock(Plan.class);
        given(plan.getAmount()).willReturn(9900);
        given(plan.getName()).willReturn("프로 플랜");

        Subscription subscription = mock(Subscription.class);
        given(subscription.getPaymentMethod()).willReturn(paymentMethod);
        given(subscription.getPlan()).willReturn(plan);
        given(subscriptionRepository.findBySubscriptionUuidAndUser_Id("sub-uuid-001", 1L))
                .willReturn(Optional.of(subscription));

        PortOneProperties.Store store = mock(PortOneProperties.Store.class);
        given(store.getId()).willReturn("store-001");
        given(portOneProperties.getStore()).willReturn(store);

        BillingKeyPaymentResponse billingResponse = mock(BillingKeyPaymentResponse.class);
        given(billingResponse.getPayment()).willReturn(null);
        given(portOneClient.billingKeyPayment(anyString(), any())).willReturn(billingResponse);

        SubscriptionBilling pendingBilling = mock(SubscriptionBilling.class);
        given(pendingBilling.getBillingHistoryUuid()).willReturn("billing-uuid-001");
        given(subscriptionBillingRepository.save(any())).willReturn(pendingBilling);

        CreateBillingRequest request = new CreateBillingRequest(
                OffsetDateTime.now(), OffsetDateTime.now().plusMonths(1)
        );

        // when
        CreateBillingResponse response = subscriptionService.createBilling(1L, "sub-uuid-001", request);

        // then: PENDING 상태 반환, 빌링 저장 1회
        assertThat(response.getStatus()).isEqualTo(SubscriptionBillingStatus.PENDING);
        verify(subscriptionBillingRepository, times(1)).save(argThat(billing ->
                billing.getStatus() == SubscriptionBillingStatus.PENDING
        ));
    }

    @Test
    @DisplayName("수동 즉시 청구 - PortOne API 예외 발생 시 FAILED 빌링이 저장되고 실패 응답을 반환한다")
    void createBilling_PortOne예외_FAILED저장및실패응답() {
        // given: PortOne API 호출 시 예외 발생
        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        given(paymentMethod.getBillingKey()).willReturn("billing-key-001");

        Plan plan = mock(Plan.class);
        given(plan.getAmount()).willReturn(9900);
        given(plan.getName()).willReturn("프로 플랜");

        Subscription subscription = mock(Subscription.class);
        given(subscription.getPaymentMethod()).willReturn(paymentMethod);
        given(subscription.getPlan()).willReturn(plan);
        given(subscriptionRepository.findBySubscriptionUuidAndUser_Id("sub-uuid-001", 1L))
                .willReturn(Optional.of(subscription));

        PortOneProperties.Store store = mock(PortOneProperties.Store.class);
        given(store.getId()).willReturn("store-001");
        given(portOneProperties.getStore()).willReturn(store);

        given(portOneClient.billingKeyPayment(anyString(), any())).willThrow(new RuntimeException("PortOne 오류"));

        SubscriptionBilling failedBilling = mock(SubscriptionBilling.class);
        given(subscriptionBillingRepository.save(any())).willReturn(failedBilling);

        CreateBillingRequest request = new CreateBillingRequest(
                OffsetDateTime.now(), OffsetDateTime.now().plusMonths(1)
        );

        // when
        CreateBillingResponse response = subscriptionService.createBilling(1L, "sub-uuid-001", request);

        // then: 실패 응답 반환, FAILED 빌링 저장 1회
//        assertThat(response.success()).isFalse();
        assertThat(response.getStatus()).isEqualTo(SubscriptionBillingStatus.FAILED);
        verify(subscriptionBillingRepository, times(1)).save(argThat(billing ->
                billing.getStatus() == SubscriptionBillingStatus.FAILED
        ));
    }
}