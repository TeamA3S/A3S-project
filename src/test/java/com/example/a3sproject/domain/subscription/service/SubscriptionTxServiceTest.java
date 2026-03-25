package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import com.example.a3sproject.domain.plan.entity.Plan;
import com.example.a3sproject.domain.plan.repository.PlanRepository;
import com.example.a3sproject.domain.subscription.dto.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dto.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.repository.SubscriptionRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.SubscriptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionTxServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PlanRepository planRepository;

    @InjectMocks
    private SubscriptionTxService subscriptionTxService;

    @Test
    @DisplayName("구독 저장 - 유효한 유저와 플랜으로 결제수단 및 구독이 정상 저장된다")
    void saveSubscription_정상요청_저장성공() {
        // given: 유저, 플랜 존재, PaymentMethod/Subscription 저장 성공
        User user = mock(User.class);
        Plan plan = mock(Plan.class);

        PaymentMethod savedPaymentMethod = mock(PaymentMethod.class);
        Subscription savedSubscription = mock(Subscription.class);
        given(savedSubscription.getSubscriptionUuid()).willReturn("sub-uuid-001");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(planRepository.findByPlanUuid("plan-uuid-001")).willReturn(Optional.of(plan));
        given(paymentMethodRepository.save(any())).willReturn(savedPaymentMethod);
        given(subscriptionRepository.save(any())).willReturn(savedSubscription);

        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "plan-uuid-001", "billing-key-001", "CUST-1", 9900
        );

        // when
        CreateSubscriptionResponse response = subscriptionTxService.saveSubscription(1L, request);

        // then: UUID 반환, 결제수단/구독 각 1회 저장
        assertThat(response.subscriptionId()).isEqualTo("sub-uuid-001");
        verify(paymentMethodRepository, times(1)).save(any(PaymentMethod.class));
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    @DisplayName("구독 저장 - 존재하지 않는 유저 ID면 SubscriptionException이 발생한다")
    void saveSubscription_유저없음_예외발생() {
        // given: 유저 조회 실패
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "plan-uuid-001", "billing-key-001", "CUST-999", 9900
        );

        // when & then
        assertThatThrownBy(() -> subscriptionTxService.saveSubscription(999L, request))
                .isInstanceOf(SubscriptionException.class)
                .satisfies(e -> assertThat(((SubscriptionException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("구독 저장 - 존재하지 않는 플랜 UUID면 SubscriptionException이 발생한다")
    void saveSubscription_플랜없음_예외발생() {
        // given: 유저 조회 성공, 플랜 조회 실패
        User user = mock(User.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(planRepository.findByPlanUuid("invalid-plan")).willReturn(Optional.empty());

        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "invalid-plan", "billing-key-001", "CUST-1", 9900
        );

        // when & then
        assertThatThrownBy(() -> subscriptionTxService.saveSubscription(1L, request))
                .isInstanceOf(SubscriptionException.class)
                .satisfies(e -> assertThat(((SubscriptionException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PLAN_NOT_FOUND));
    }
}