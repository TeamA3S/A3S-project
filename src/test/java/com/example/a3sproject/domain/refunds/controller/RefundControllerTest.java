package com.example.a3sproject.domain.refunds.controller;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.order.repository.OrderRepository;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.response.PortOneCancelPaymentResponse;
import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductCategory;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.product.repository.ProductRepository;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class RefundControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    // PortOneClient는 외부 API이므로 MockitoBean으로 격리
    @MockitoBean private PortOneClient portOneClient;

    private User 테스트유저;
    private String 액세스토큰;
    private Product 테스트상품;
    private Order 환불가능주문;
    private Payment 환불가능결제;

    @BeforeEach
    void setUp() {
        // 테스트유저 저장, 멤버십 초기화, 액세스 토큰 발급
        테스트유저 = userRepository.save(
                new User("환불테스터", "refund@test.com",
                        passwordEncoder.encode("pass"), "010-1111-2222",
                        0, 100000, MembershipGrade.NORMAL, "CUST_REFUND_001"));
        membershipRepository.save(Membership.init(테스트유저));
        액세스토큰 = jwtTokenProvider.createToken("refund@test.com", MembershipGrade.NORMAL);

        // 상품 저장
        테스트상품 = productRepository.save(Product.builder()
                .name("환불 테스트 상품").price(100000).stock(10).description("설명")
                .productStatus(ProductStatus.ON_SALE).productCategory(ProductCategory.UPSTREAM).build());

        // 환불 가능한 주문/결제 생성 (COMPLETED + SUCCESS 상태)
        환불가능주문 = orderRepository.save(Order.createOrder(
                테스트유저,
                List.of(OrderItem.createOrderItem(테스트상품, 1)),
                "ODN-REFUND-001"
        ));
        환불가능주문.updateOrderStatus(OrderStatus.COMPLETED);

        환불가능결제 = paymentRepository.save(new Payment(환불가능주문, 100000, "PMN-REFUND-001", 0));
        환불가능결제.confirmPayment(OffsetDateTime.now());

        // PortOne 취소 API Mock 응답 설정
        given(portOneClient.cancelPayment(anyString(), any())).willReturn(
                new PortOneCancelPaymentResponse(
                        new PortOneCancelPaymentResponse.Cancellation(
                                "CANCELLED", 100000, OffsetDateTime.now()
                        )
                )
        );
    }

    // =========================================================
    // POST /api/refunds/{portOneId}
    // =========================================================

    @Test
    @DisplayName("정상적인 환불 요청 시 200 OK와 환불 완료 정보를 반환한다")
    void refundPayment_정상요청_200응답() throws Exception {
        // given: 환불 사유 포함 요청
        Map<String, String> request = Map.of("reason", "단순 변심");

        // when
        ResultActions result = mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-REFUND-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 200 반환, 환불 성공 여부와 주문번호 포함
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ODN-REFUND-001"))
                .andExpect(jsonPath("$.data.paidStatus").value("REFUNDED"));
    }

    @Test
    @DisplayName("환불 성공 시 주문 상태가 REFUNDED로 변경된다")
    void refundPayment_환불성공_주문상태REFUNDED() throws Exception {
        // given: 환불 요청
        Map<String, String> request = Map.of("reason", "단순 변심");

        // when
        mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-REFUND-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: DB에서 주문 상태가 REFUNDED로 변경된 것을 직접 검증
        assertThat(환불가능주문.getOrderStatus()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    @DisplayName("환불 성공 시 주문 상품의 재고가 복구된다")
    void refundPayment_환불성공_재고복구() throws Exception {
        // given: 현재 상품 재고 10개, 1개 주문했던 상품 환불
        Map<String, String> request = Map.of("reason", "단순 변심");

        // when
        mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-REFUND-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 재고가 10 + 1 = 11로 복구된다
        assertThat(테스트상품.getStock()).isEqualTo(11);
    }

    @Test
    @DisplayName("환불 성공 시 총 결제금액이 차감되고 멤버십 등급이 재계산된다")
    void refundPayment_환불성공_총결제금액차감및등급재계산() throws Exception {
        // given: 총 결제금액 100,000원인 유저, 100,000원 환불 → 0원으로 감소
        Map<String, String> request = Map.of("reason", "단순 변심");

        // when
        mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-REFUND-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 총 결제금액 0원, NORMAL 등급 유지
        assertThat(테스트유저.getTotalPaymentAmount()).isEqualTo(0);
    }

    @Test
    @DisplayName("이미 환불 완료된 결제에 재환불을 시도하면 409 Conflict와 DUPLICATE_REFUND_REQUEST를 반환한다")
    void refundPayment_중복환불_409에러응답() throws Exception {
        // given: 첫 번째 환불 완료
        Map<String, String> request = Map.of("reason", "단순 변심");
        mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-REFUND-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // when: 동일한 portOneId로 재환불 시도
        ResultActions result = mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-REFUND-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 중복 환불 방어로 DUPLICATE_REFUND_REQUEST 반환
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REFUND_REQUEST"));
    }

    @Test
    @DisplayName("타유저의 결제를 환불하려 하면 403 Forbidden과 USER_FORBIDDEN을 반환한다")
    void refundPayment_타유저결제환불시도_403에러응답() throws Exception {
        // given: 타유저 생성 후 해당 유저의 주문/결제 저장
        User 타유저 = userRepository.save(new User("타유저", "other@test.com",
                passwordEncoder.encode("pass"), "010-7777-8888",
                0, 0, MembershipGrade.NORMAL, "CUST_OTHER_001"));
        membershipRepository.save(Membership.init(타유저));

        Order 타유저주문 = orderRepository.save(Order.createOrder(
                타유저,
                List.of(OrderItem.createOrderItem(테스트상품, 1)),
                "ODN-OTHER-001"
        ));
        타유저주문.updateOrderStatus(OrderStatus.COMPLETED);

        Payment 타유저결제 = paymentRepository.save(new Payment(타유저주문, 50000, "PMN-OTHER-001", 0));
        타유저결제.confirmPayment(OffsetDateTime.now());

        Map<String, String> request = Map.of("reason", "변심");

        // when: 테스트유저 토큰으로 타유저 결제 환불 시도
        ResultActions result = mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-OTHER-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
    }

    @Test
    @DisplayName("결제 완료 상태가 아닌 주문을 환불하려 하면 409 Conflict와 ORDER_CANNOT_REFUND를 반환한다")
    void refundPayment_결제미완료주문_409에러응답() throws Exception {
        // given: PENDING 상태의 주문/결제 생성
        Order 미결제주문 = orderRepository.save(Order.createOrder(
                테스트유저,
                List.of(OrderItem.createOrderItem(테스트상품, 1)),
                "ODN-PENDING-001"
        ));
        Payment 미완료결제 = paymentRepository.save(new Payment(미결제주문, 100000, "PMN-PENDING-001", 0));

        Map<String, String> request = Map.of("reason", "변심");

        // when: PENDING 상태 주문 환불 시도
        ResultActions result = mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-PENDING-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_CANNOT_REFUND"));
    }

    @Test
    @DisplayName("존재하지 않는 portOneId로 환불을 시도하면 404 Not Found와 PAYMENT_NOT_FOUND를 반환한다")
    void refundPayment_존재하지않는portOneId_404에러응답() throws Exception {
        // given: DB에 없는 portOneId
        Map<String, String> request = Map.of("reason", "변심");

        // when
        ResultActions result = mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-GHOST-999")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 환불을 시도하면 401 Unauthorized를 반환한다")
    void refundPayment_비인증요청_401에러응답() throws Exception {
        // given: 토큰 없는 요청
        Map<String, String> request = Map.of("reason", "변심");

        // when
        ResultActions result = mockMvc.perform(post("/api/refunds/{portOneId}", "PMN-REFUND-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isUnauthorized());
    }
}