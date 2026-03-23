package com.example.a3sproject.domain.payment.controller;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.order.repository.OrderRepository;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.PortOnePaymentResponse;
import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    // PortOneClient는 외부 API이므로 MockBean으로 격리
    @MockitoBean
    private PortOneClient portOneClient;

    private User 테스트유저;
    private String 액세스토큰;
    private Order 결제대기주문;
    private Order 이미완료된주문;

    @BeforeEach
    void setUp() {
        // 테스트유저 저장, 멤버십 초기화, 액세스 토큰 발급
        테스트유저 = userRepository.save(
                new User("결제테스터", "payment@test.com",
                        passwordEncoder.encode("pass"), "010-1111-2222",
                        50000, 0, MembershipGrade.NORMAL, "CUST_PAY_001"));
        membershipRepository.save(Membership.init(테스트유저));
        액세스토큰 = jwtTokenProvider.createToken("payment@test.com", MembershipGrade.NORMAL);

        // 상품 및 주문 데이터 저장
        Product 상품 = productRepository.save(Product.builder()
                .name("테스트 상품").price(100000).stock(10).description("설명")
                .productStatus(ProductStatus.ON_SALE).productCategory(ProductCategory.UPSTREAM).build());

        결제대기주문 = orderRepository.save(Order.createOrder(
                테스트유저,
                List.of(OrderItem.createOrderItem(상품, 1)),
                "ODN-PAY-001"
        ));

        이미완료된주문 = orderRepository.save(Order.createOrder(
                테스트유저,
                List.of(OrderItem.createOrderItem(상품, 1)),
                "ODN-PAY-002"
        ));
        // 이미 완료된 주문 상태 변경
        이미완료된주문.updateOrderStatus(OrderStatus.COMPLETED);
    }

    // =========================================================
    // POST /api/payments/attempt
    // =========================================================

    @Test
    @DisplayName("정상적인 결제 시도 요청 시 201 Created와 portOneId가 반환된다")
    void createPayment_정상요청_201응답() throws Exception {
        // given: 결제 대기 주문에 포인트 미사용 결제 시도
        Map<String, Object> request = Map.of(
                "orderId", 결제대기주문.getId(),
                "totalAmount", 100000,
                "pointsToUse", 0
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/attempt")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 201 반환, portOneId와 PENDING 상태 포함
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentId").exists())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(100000));
    }

    @Test
    @DisplayName("포인트 일부 사용 복합 결제 시도 시 포인트 차감 후 잔여 금액으로 결제가 생성된다")
    void createPayment_포인트일부사용_복합결제생성() throws Exception {
        // given: 주문 100,000원, 포인트 30,000P 사용 → 실결제 70,000원
        Map<String, Object> request = Map.of(
                "orderId", 결제대기주문.getId(),
                "totalAmount", 100000,
                "pointsToUse", 30000
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/attempt")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 실결제 금액이 70,000원으로 차감되어 반환된다
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(70000));
    }

    @Test
    @DisplayName("포인트 전액 결제 시 즉시 완료 처리되어 SUCCESS 상태가 반환된다")
    void createPayment_포인트전액결제_즉시완료() throws Exception {
        // given: 주문 100,000원, 포인트 100,000P 전액 사용
        // 유저 포인트 잔액을 100,000P로 충분하게 설정
        테스트유저 = userRepository.save(
                new User("포인트부자", "rich@test.com",
                        passwordEncoder.encode("pass"), "010-3333-4444",
                        100000, 0, MembershipGrade.NORMAL, "CUST_PAY_002"));
        membershipRepository.save(Membership.init(테스트유저));
        String 포인트부자토큰 = jwtTokenProvider.createToken("rich@test.com", MembershipGrade.NORMAL);

        Product 상품 = productRepository.save(Product.builder()
                .name("포인트상품").price(100000).stock(5).description("설명")
                .productStatus(ProductStatus.ON_SALE).productCategory(ProductCategory.UPSTREAM).build());
        Order 포인트주문 = orderRepository.save(Order.createOrder(
                테스트유저,
                List.of(OrderItem.createOrderItem(상품, 1)),
                "ODN-POINT-001"
        ));

        Map<String, Object> request = Map.of(
                "orderId", 포인트주문.getId(),
                "totalAmount", 100000,
                "pointsToUse", 100000
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/attempt")
                .header("Authorization", "Bearer " + 포인트부자토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 실결제 금액이 0원이고 즉시 SUCCESS 상태로 완료된다
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(0))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @DisplayName("클라이언트 금액이 서버 주문 금액과 다르면 400 Bad Request와 PAYMENT_AMOUNT_MISMATCH를 반환한다")
    void createPayment_금액불일치_400에러응답() throws Exception {
        // given: 실제 주문금액 100,000원인데 클라이언트가 90,000원으로 전송
        Map<String, Object> request = Map.of(
                "orderId", 결제대기주문.getId(),
                "totalAmount", 90000,
                "pointsToUse", 0
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/attempt")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_AMOUNT_MISMATCH"));
    }

    @Test
    @DisplayName("이미 완료된 주문에 결제를 시도하면 409 Conflict와 DUPLICATE_PAYMENT_REQUEST를 반환한다")
    void createPayment_이미완료된주문_409에러응답() throws Exception {
        // given: COMPLETED 상태인 주문에 결제 시도
        Map<String, Object> request = Map.of(
                "orderId", 이미완료된주문.getId(),
                "totalAmount", 100000,
                "pointsToUse", 0
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/attempt")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PAYMENT_REQUEST"));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 결제 시도 시 401 Unauthorized를 반환한다")
    void createPayment_비인증요청_401에러응답() throws Exception {
        // given: 토큰 없는 요청
        Map<String, Object> request = Map.of(
                "orderId", 결제대기주문.getId(),
                "totalAmount", 100000,
                "pointsToUse", 0
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/attempt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isUnauthorized());
    }

    // =========================================================
    // POST /api/payments/{paymentId}/confirm
    // =========================================================

    @Test
    @DisplayName("PortOne 검증을 통과하면 200 OK와 함께 결제가 확정되고 주문이 COMPLETED 상태로 변경된다")
    void confirmPayment_정상검증_200응답및주문완료() throws Exception {
        // given: 결제 시도 후 생성된 Payment 객체를 DB에 저장, PortOne Mock 응답 설정
        Payment payment = paymentRepository.save(
                new Payment(결제대기주문, 100000, "PMN-CONFIRM-001", 0));

        given(portOneClient.getPayment("PMN-CONFIRM-001")).willReturn(
                new PortOnePaymentResponse(
                        "PMN-CONFIRM-001",
                        PortOnePayStatus.PAID,
                        new PortOnePaymentResponse.PaymentAmount(100000, 0),
                        OffsetDateTime.now()
                )
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/{paymentId}/confirm", "PMN-CONFIRM-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 200 반환, 주문번호 포함, DB에서 주문 상태 COMPLETED 검증
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("ODN-PAY-001"));

        assertThat(결제대기주문.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("PortOne 결제 상태가 FAILED이면 500 PAYMENT_PORTONE_ERROR를 반환한다")
    void confirmPayment_PortOne결제실패상태_500에러응답() throws Exception {
        // given: PortOne에서 FAILED 상태 응답
        paymentRepository.save(new Payment(결제대기주문, 100000, "PMN-FAILED-001", 0));

        given(portOneClient.getPayment("PMN-FAILED-001")).willReturn(
                new PortOnePaymentResponse(
                        "PMN-FAILED-001",
                        PortOnePayStatus.FAILED,
                        new PortOnePaymentResponse.PaymentAmount(100000, 0),
                        OffsetDateTime.now()
                )
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/{paymentId}/confirm", "PMN-FAILED-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: PortOne 결제 상태 검증 실패로 PAYMENT_PORTONE_ERROR 반환
        result.andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PAYMENT_PORTONE_ERROR"));
    }

    @Test
    @DisplayName("동일한 결제를 중복 확정하면 409 Conflict와 DUPLICATE_PAYMENT_REQUEST를 반환한다")
    void confirmPayment_중복확정_409에러응답() throws Exception {
        // given: 이미 SUCCESS 상태인 Payment 저장
        Payment payment = new Payment(결제대기주문, 100000, "PMN-DUP-001", 0);
        payment.confirmPayment(OffsetDateTime.now());
        paymentRepository.save(payment);

        // when: 동일한 paymentId로 재확정 시도
        ResultActions result = mockMvc.perform(post("/api/payments/{paymentId}/confirm", "PMN-DUP-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PAYMENT_REQUEST"));
    }

    @Test
    @DisplayName("존재하지 않는 paymentId로 확정을 시도하면 404 Not Found와 PAYMENT_NOT_FOUND를 반환한다")
    void confirmPayment_존재하지않는ID_404에러응답() throws Exception {
        // given: DB에 없는 paymentId

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/{paymentId}/confirm", "PMN-GHOST-999")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("타유저의 결제를 확정하려 하면 403 Forbidden과 USER_FORBIDDEN을 반환한다")
    void confirmPayment_타유저결제확정시도_403에러응답() throws Exception {
        // given: 타유저 생성 후 해당 유저의 주문/결제 저장
        User 타유저 = userRepository.save(new User("타유저", "other@test.com",
                passwordEncoder.encode("pass"), "010-7777-8888",
                0, 0, MembershipGrade.NORMAL, "CUST_OTHER_001"));
        membershipRepository.save(Membership.init(타유저));

        Product 상품 = productRepository.save(Product.builder()
                .name("타유저상품").price(50000).stock(5).description("설명")
                .productStatus(ProductStatus.ON_SALE).productCategory(ProductCategory.UPSTREAM).build());
        Order 타유저주문 = orderRepository.save(Order.createOrder(
                타유저,
                List.of(OrderItem.createOrderItem(상품, 1)),
                "ODN-OTHER-001"
        ));
        paymentRepository.save(new Payment(타유저주문, 50000, "PMN-OTHER-001", 0));

        // when: 테스트유저 토큰으로 타유저 결제 확정 시도
        ResultActions result = mockMvc.perform(post("/api/payments/{paymentId}/confirm", "PMN-OTHER-001")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 결제 확정을 시도하면 401 Unauthorized를 반환한다")
    void confirmPayment_비인증요청_401에러응답() throws Exception {
        // given: 토큰 없는 요청

        // when
        ResultActions result = mockMvc.perform(post("/api/payments/{paymentId}/confirm", "PMN-ANY-001")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized());
    }
}