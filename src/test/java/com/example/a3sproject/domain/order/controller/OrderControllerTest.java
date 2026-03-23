package com.example.a3sproject.domain.order.controller;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.repository.OrderRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User 테스트유저;
    private String 액세스토큰;
    private Product 판매중상품A;
    private Product 판매중상품B;
    private Product 품절상품;

    @BeforeEach
    void setUp() {
        // 테스트유저 저장, 멤버십 초기화, 액세스 토큰 발급
        테스트유저 = userRepository.save(
                new User("주문테스터", "order@test.com",
                        passwordEncoder.encode("pass"), "010-1111-2222",
                        0, 0, MembershipGrade.NORMAL, "CUST_ORDER_001"));
        membershipRepository.save(Membership.init(테스트유저));
        액세스토큰 = jwtTokenProvider.createToken("order@test.com", MembershipGrade.NORMAL);

        // 상품 데이터 저장
        판매중상품A = productRepository.save(Product.builder()
                .name("나이키 신발").price(100000).stock(10).description("신발")
                .productStatus(ProductStatus.ON_SALE).productCategory(ProductCategory.UPSTREAM).build());
        판매중상품B = productRepository.save(Product.builder()
                .name("아디다스 양말").price(5000).stock(50).description("양말")
                .productStatus(ProductStatus.ON_SALE).productCategory(ProductCategory.MIDSTREAM).build());
        품절상품 = productRepository.save(Product.builder()
                .name("품절 모자").price(20000).stock(0).description("모자")
                .productStatus(ProductStatus.SOLD_OUT).productCategory(ProductCategory.DOWNSTREAM).build());
    }

    // =========================================================
    // POST /api/orders
    // =========================================================

    @Test
    @DisplayName("인증된 유저가 정상적인 주문 요청을 하면 201 Created와 주문 정보를 반환한다")
    void createOrder_정상요청_201응답() throws Exception {
        // given: 판매 중인 상품 2종 주문 요청
        Map<String, Object> request = Map.of(
                "items", List.of(
                        Map.of("productId", 판매중상품A.getId(), "quantity", 2),
                        Map.of("productId", 판매중상품B.getId(), "quantity", 3)
                )
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 201 반환, orderId/totalAmount/orderNumber 모두 응답에 포함된다
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.data.totalAmount").value(215000))
                .andExpect(jsonPath("$.data.orderNumber").exists());
    }

    @Test
    @DisplayName("주문 생성 시 totalAmount는 단가 × 수량의 합산으로 정확하게 계산된다")
    void createOrder_정상요청_totalAmount정확히계산() throws Exception {
        // given: 상품A(100,000원) × 1개만 주문
        Map<String, Object> request = Map.of(
                "items", List.of(Map.of("productId", 판매중상품A.getId(), "quantity", 1))
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: totalAmount가 100,000 × 1 = 100,000으로 계산된다
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(100000));
    }

    @Test
    @DisplayName("동일한 상품을 중복 요청하면 수량이 병합되어 하나의 주문 항목으로 처리된다")
    void createOrder_중복상품요청_수량병합처리() throws Exception {
        // given: 상품A를 2개, 1개로 나누어 중복 요청 (총 3개로 병합되어야 함)
        Map<String, Object> request = Map.of(
                "items", List.of(
                        Map.of("productId", 판매중상품A.getId(), "quantity", 2),
                        Map.of("productId", 판매중상품A.getId(), "quantity", 1)
                )
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 100,000 × 3 = 300,000으로 totalAmount 계산
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(300000));
    }

    @Test
    @DisplayName("품절 상태의 상품을 주문하면 400 Bad Request와 ORDERITEM_UNAVAILABLE을 반환한다")
    void createOrder_품절상품주문_400에러응답() throws Exception {
        // given: SOLD_OUT 상태의 상품 주문 시도
        Map<String, Object> request = Map.of(
                "items", List.of(Map.of("productId", 품절상품.getId(), "quantity", 1))
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDERITEM_UNAVAILABLE"));
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 409 Conflict와 PRODUCT_OUT_OF_STOCK을 반환한다")
    void createOrder_재고초과주문_409에러응답() throws Exception {
        // given: 현재 재고(10개)를 초과하는 수량(99개) 주문 시도
        Map<String, Object> request = Map.of(
                "items", List.of(Map.of("productId", 판매중상품A.getId(), "quantity", 99))
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_OUT_OF_STOCK"));
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 주문하면 404 Not Found와 ORDERITEM_NOT_FOUND를 반환한다")
    void createOrder_존재하지않는상품_404에러응답() throws Exception {
        // given: DB에 없는 상품 ID 주문 시도
        Map<String, Object> request = Map.of(
                "items", List.of(Map.of("productId", 99999L, "quantity", 1))
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDERITEM_NOT_FOUND"));
    }

    @Test
    @DisplayName("주문 아이템 없이 요청하면 400 Bad Request와 INVALID_INPUT을 반환한다")
    void createOrder_빈아이템목록_400에러응답() throws Exception {
        // given: items 배열이 비어있는 요청
        Map<String, Object> request = Map.of("items", List.of());

        // when
        ResultActions result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 주문 생성을 시도하면 401 Unauthorized를 반환한다")
    void createOrder_비인증요청_401에러응답() throws Exception {
        // given: 토큰 없는 요청
        Map<String, Object> request = Map.of(
                "items", List.of(Map.of("productId", 판매중상품A.getId(), "quantity", 1))
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: Security 필터가 실제 동작하여 인증 실패 감지
        result.andExpect(status().isUnauthorized());
    }

    // =========================================================
    // GET /api/orders
    // =========================================================

    @Test
    @DisplayName("인증된 유저가 주문 목록을 조회하면 200 OK와 본인의 주문 목록을 반환한다")
    void getAllOrderList_정상요청_200응답() throws Exception {
        // given: 테스트유저의 주문 2건 생성
        createOrderDirectly("ODN-LIST-001");
        createOrderDirectly("ODN-LIST-002");

        // when
        ResultActions result = mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 2건의 주문이 반환된다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("주문이 없는 유저가 목록 조회 시 200 OK와 빈 배열을 반환한다")
    void getAllOrderList_주문없음_200빈배열반환() throws Exception {
        // given: 해당 유저의 주문이 없는 상태 (setUp에서 주문 생성 없음)

        // when
        ResultActions result = mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 404가 아닌 200 OK와 빈 배열 반환
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("다른 유저의 주문은 내 주문 목록에 포함되지 않는다")
    void getAllOrderList_타유저주문분리_본인주문만반환() throws Exception {
        // given: 타유저 생성 후 주문 생성, 테스트유저도 주문 1건 생성
        User 타유저 = userRepository.save(new User("타유저", "other@test.com",
                passwordEncoder.encode("pass"), "010-7777-8888",
                0, 0, MembershipGrade.NORMAL, "CUST_OTHER_001"));
        membershipRepository.save(Membership.init(타유저));

        orderRepository.save(Order.createOrder(타유저,
                List.of(createOrderItemHelper(판매중상품A, 1)), "ODN-OTHER-001"));
        createOrderDirectly("ODN-MINE-001");

        // when: 테스트유저 토큰으로 조회
        ResultActions result = mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 테스트유저 주문 1건만 반환되며 타유저 주문은 포함되지 않는다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].orderNumber").value("ODN-MINE-001"));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 주문 목록 조회를 시도하면 401 Unauthorized를 반환한다")
    void getAllOrderList_비인증요청_401에러응답() throws Exception {
        // given: 토큰 없는 요청

        // when
        ResultActions result = mockMvc.perform(get("/api/orders")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized());
    }

    // =========================================================
    // GET /api/orders/{orderId}
    // =========================================================

    @Test
    @DisplayName("본인의 주문 ID로 상세 조회하면 200 OK와 주문 상세 정보를 반환한다")
    void getOrderDetail_정상요청_200응답() throws Exception {
        // given: 테스트유저의 주문 1건 생성
        Order order = createOrderDirectly("ODN-DETAIL-001");

        // when
        ResultActions result = mockMvc.perform(get("/api/orders/{orderId}", order.getId())
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 주문 기본 정보 및 orderItems가 포함된 상세 응답 반환
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(order.getId()))
                .andExpect(jsonPath("$.data.orderNumber").value("ODN-DETAIL-001"))
                .andExpect(jsonPath("$.data.totalAmount").value(100000))
                .andExpect(jsonPath("$.data.orderItems").isArray())
                .andExpect(jsonPath("$.data.orderItems", hasSize(1)));
    }

    @Test
    @DisplayName("타유저의 주문 ID로 상세 조회를 시도하면 404 Not Found와 ORDER_NOT_FOUND를 반환한다")
    void getOrderDetail_타유저주문조회_404에러응답() throws Exception {
        // given: 타유저 생성 후 해당 유저의 주문 생성
        User 타유저 = userRepository.save(new User("타유저", "other2@test.com",
                passwordEncoder.encode("pass"), "010-5555-6666",
                0, 0, MembershipGrade.NORMAL, "CUST_OTHER_002"));
        membershipRepository.save(Membership.init(타유저));
        Order 타유저주문 = orderRepository.save(Order.createOrder(타유저,
                List.of(createOrderItemHelper(판매중상품A, 1)), "ODN-OTHER-002"));

        // when: 테스트유저 토큰으로 타유저 주문 조회 시도
        ResultActions result = mockMvc.perform(get("/api/orders/{orderId}", 타유저주문.getId())
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 소유권 검증 실패로 ORDER_NOT_FOUND 반환 (타유저 주문 존재 여부 노출 방지)
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 상세 조회를 시도하면 404 Not Found와 ORDER_NOT_FOUND를 반환한다")
    void getOrderDetail_존재하지않는주문_404에러응답() throws Exception {
        // given: DB에 없는 주문 ID
        Long nonExistentOrderId = 99999L;

        // when
        ResultActions result = mockMvc.perform(get("/api/orders/{orderId}", nonExistentOrderId)
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 주문 상세 조회를 시도하면 401 Unauthorized를 반환한다")
    void getOrderDetail_비인증요청_401에러응답() throws Exception {
        // given: 토큰 없는 요청
        Order order = createOrderDirectly("ODN-UNAUTH-001");

        // when
        ResultActions result = mockMvc.perform(get("/api/orders/{orderId}", order.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized());
    }

    // =========================================================
    // 테스트 헬퍼 메서드
    // =========================================================

    private Order createOrderDirectly(String orderNumber) {
        return orderRepository.save(Order.createOrder(
                테스트유저,
                List.of(createOrderItemHelper(판매중상품A, 1)),
                orderNumber
        ));
    }

    private com.example.a3sproject.domain.order.entity.OrderItem createOrderItemHelper(Product product, int quantity) {
        return com.example.a3sproject.domain.order.entity.OrderItem.createOrderItem(product, quantity);
    }
}