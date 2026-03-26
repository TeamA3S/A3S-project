package com.example.a3sproject.domain.subscription.controller;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.paymentMethod.enums.PaymentMethodStatus;
import com.example.a3sproject.domain.paymentMethod.enums.PgProvider;
import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import com.example.a3sproject.domain.plan.entity.Plan;
import com.example.a3sproject.domain.plan.repository.PlanRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.response.BillingKeyPaymentResponse;
import com.example.a3sproject.domain.portone.dto.response.ValidateBillingKeyResponse;
import com.example.a3sproject.domain.subscription.dto.request.CreateBillingRequest;
import com.example.a3sproject.domain.subscription.dto.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dto.request.UpdateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.example.a3sproject.domain.subscription.repository.SubscriptionBillingRepository;
import com.example.a3sproject.domain.subscription.repository.SubscriptionRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class SubscriptionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private PaymentMethodRepository paymentMethodRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionBillingRepository subscriptionBillingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private PortOneClient portOneClient;

    private User 테스트유저;
    private String 액세스토큰;
    private Plan 베이직플랜;
    private PaymentMethod 기존결제수단;

    @BeforeEach
    void setUp() {
        // 테스트 유저 생성 및 토큰 발급
        테스트유저 = userRepository.save(new User("구독테스터", "sub@test.com",
                passwordEncoder.encode("pass"), "010-1234-5678", "CUST-SUB-001"));
        membershipRepository.save(Membership.init(테스트유저));
        액세스토큰 = jwtTokenProvider.createToken("sub@test.com", MembershipGrade.NORMAL);

        // 플랜 생성
        베이직플랜 = planRepository.save(new Plan("베이직 플랜", 9900, "MONTHLY", true));

        // 기존 결제수단 (조회/해지용)
        기존결제수단 = paymentMethodRepository.save(new PaymentMethod(
                테스트유저, "bill-key-old", "CUST-SUB-001", PgProvider.TOSS_PAYMENTS, true, PaymentMethodStatus.ACTIVE
        ));

        // PortOne Mock 기본 응답 설정 (빌링키 검증 성공)
        ValidateBillingKeyResponse validResponse = new ValidateBillingKeyResponse(
                "ISSUED", "bill-key-new", "MERCHANT-ID", "STORE-ID"
        );
        given(portOneClient.getBillingKey(anyString())).willReturn(validResponse);

        // PortOne Mock 기본 응답 설정 (결제 성공)
        BillingKeyPaymentResponse.PaymentDetails paymentDetails = mock(BillingKeyPaymentResponse.PaymentDetails.class);
        given(paymentDetails.getPaidAt()).willReturn(OffsetDateTime.now().toString());

        BillingKeyPaymentResponse billingResponse = mock(BillingKeyPaymentResponse.class);
        given(billingResponse.getPayment()).willReturn(paymentDetails);
        given(portOneClient.billingKeyPayment(anyString(), any())).willReturn(billingResponse);
    }

    // =========================================================
    // POST /api/subscriptions
    // =========================================================

    @Test
    @DisplayName("정상적인 구독 생성 요청 시 201 Created와 구독 정보를 반환한다")
    void createSubscription_정상요청_201응답() throws Exception {
        // given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "CUST-SUB-001", 베이직플랜.getPlanUuid(), "bill-key-new", 9900
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/subscriptions")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subscriptionId").exists());

        // DB 검증
        boolean exists = subscriptionRepository.existsByUserAndPlanAndStatus(테스트유저, 베이직플랜, SubscriptionStatus.ACTIVE);
        assertThat(exists).isTrue();
    }

    // =========================================================
    // GET /api/subscriptions/{subscriptionId}
    // =========================================================

    @Test
    @DisplayName("본인의 구독 정보를 조회하면 200 OK와 상세 정보를 반환한다")
    void getSubscription_정상조회_200응답() throws Exception {
        // given: 구독 생성
        Subscription subscription = subscriptionRepository.save(new Subscription(
                테스트유저, 베이직플랜, 기존결제수단, 9900
        ));

        // when
        ResultActions result = mockMvc.perform(get("/api/subscriptions/{subscriptionId}", subscription.getSubscriptionUuid())
                .header("Authorization", "Bearer " + 액세스토큰));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionId").value(subscription.getSubscriptionUuid()))
                .andExpect(jsonPath("$.data.amount").value(9900))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    // =========================================================
    // GET /api/subscriptions/{subscriptionId}/billings
    // =========================================================

    @Test
    @DisplayName("구독의 결제 내역을 조회하면 200 OK와 목록을 반환한다")
    void getBillings_정상조회_200응답() throws Exception {
        // given: 구독 및 결제 내역 생성
        Subscription subscription = subscriptionRepository.save(new Subscription(
                테스트유저, 베이직플랜, 기존결제수단, 9900
        ));
        subscriptionBillingRepository.save(new SubscriptionBilling(
                subscription, 9900, SubscriptionBillingStatus.PAID, "PMN-HIST-001",
                OffsetDateTime.now().minusMonths(1), OffsetDateTime.now(), null
        ));

        // when
        ResultActions result = mockMvc.perform(get("/api/subscriptions/{subscriptionId}/billings", subscription.getSubscriptionUuid())
                .header("Authorization", "Bearer " + 액세스토큰));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billings").isArray())
                .andExpect(jsonPath("$.data.billings[0].paymentId").value("PMN-HIST-001"));
    }

    // =========================================================
    // POST /api/subscriptions/{subscriptionId}/billings
    // =========================================================

    @Test
    @DisplayName("수동 즉시 청구 요청 시 200 OK와 결제 결과를 반환한다")
    void createBilling_정상요청_200응답() throws Exception {
        // given
        Subscription subscription = subscriptionRepository.save(new Subscription(
                테스트유저, 베이직플랜, 기존결제수단, 9900
        ));
        CreateBillingRequest request = new CreateBillingRequest(
                OffsetDateTime.now(), OffsetDateTime.now().plusMonths(1)
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/subscriptions/{subscriptionId}/billings", subscription.getSubscriptionUuid())
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    // =========================================================
    // PATCH /api/subscriptions/{subscriptionId}
    // =========================================================

    @Test
    @DisplayName("구독 해지 요청 시 200 OK를 반환하고 구독 상태가 CANCELLED로 변경된다")
    void cancelSubscription_정상요청_200응답() throws Exception {
        // given
        Subscription subscription = subscriptionRepository.save(new Subscription(
                테스트유저, 베이직플랜, 기존결제수단, 9900
        ));
        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest("cancel", "단순 변심");

        // when
        ResultActions result = mockMvc.perform(patch("/api/subscriptions/{subscriptionId}", subscription.getSubscriptionUuid())
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk());

        // DB 검증
        Subscription updated = subscriptionRepository.findBySubscriptionUuid(subscription.getSubscriptionUuid()).get();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(updated.getCanceledAt()).isNotNull();
    }
}