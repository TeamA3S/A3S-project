package com.example.a3sproject.domain.order.service;

import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.order.dto.CreateOrderRequestDto;
import com.example.a3sproject.domain.order.dto.CreateOrderResponseDto;
import com.example.a3sproject.domain.order.dto.GetOrderDetailResponseDto;
import com.example.a3sproject.domain.order.dto.GetOrderListResponseDto;
import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.repository.OrderRepository;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductCategory;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.OrderException;
import com.example.a3sproject.global.exception.domain.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PointRepository pointRepository;

    @InjectMocks
    private OrderService orderService;

    private final User 테스트유저 = new User("홍길동", "test@test.com", "pass", "010-0000-0000", 0, 0, MembershipGrade.NORMAL, "CUST_001");

    private final Product 상품A = createProduct(1L, "나이키 신발", 100000, 10, ProductStatus.ON_SALE);
    private final Product 상품B = createProduct(2L, "아디다스 양말", 5000, 50, ProductStatus.ON_SALE);
    private final Product 품절상품 = createProduct(3L, "품절 모자", 20000, 0, ProductStatus.SOLD_OUT);

    private Product createProduct(Long id, String name, int price, int stock, ProductStatus status) {
        Product product = Product.builder()
                .name(name).price(price).stock(stock).description("설명")
                .productStatus(status).productCategory(ProductCategory.UPSTREAM).build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private CreateOrderRequestDto.OrderItemRequestDto createItemRequest(Long productId, Integer quantity) {
        CreateOrderRequestDto.OrderItemRequestDto item = new CreateOrderRequestDto.OrderItemRequestDto();
        ReflectionTestUtils.setField(item, "productId", productId);
        ReflectionTestUtils.setField(item, "quantity", quantity);
        return item;
    }

    private CreateOrderRequestDto createOrderRequest(List<CreateOrderRequestDto.OrderItemRequestDto> items) {
        CreateOrderRequestDto request = new CreateOrderRequestDto();
        ReflectionTestUtils.setField(request, "orderItems", items);
        return request;
    }

    private <T> T createInstance(Class<T> clazz) {
        try {
            java.lang.reflect.Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("정상적인 다중 상품 주문 요청 시 주문이 생성되고 결과가 반환된다")
    void createOrder_정상입력_주문생성성공() {
        // given: 유저 조회 성공, 두 개의 다른 상품 조회 및 재고 확인 성공, 주문 저장
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        CreateOrderRequestDto request = createOrderRequest(List.of(
                createItemRequest(1L, 2),
                createItemRequest(2L, 3)
        ));
        given(userRepository.findById(1L)).willReturn(Optional.of(테스트유저));
        given(productRepository.findAllById(Set.of(1L, 2L))).willReturn(List.of(상품A, 상품B));

        Order savedOrder = createInstance(Order.class);
        ReflectionTestUtils.setField(savedOrder, "id", 100L);
        ReflectionTestUtils.setField(savedOrder, "orderNumber", "ODN-TEST-123");
        ReflectionTestUtils.setField(savedOrder, "totalAmount", 215000);
        ReflectionTestUtils.setField(savedOrder, "orderStatus", com.example.a3sproject.domain.order.enums.OrderStatus.PENDING);
        ReflectionTestUtils.setField(savedOrder, "user", 테스트유저);
        ReflectionTestUtils.setField(savedOrder, "orderItems", new java.util.ArrayList<>());
        ReflectionTestUtils.setField(savedOrder, "usedPointAmount", 0);
        ReflectionTestUtils.setField(savedOrder, "finalAmount", 215000);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        CreateOrderResponseDto result = orderService.createOrder(1L, request);

        // then: 생성된 주문의 ID, 총액, 주문번호가 반환되고 DB 저장이 1회 호출된다
        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.getTotalAmount()).isEqualTo(215000);
        assertThat(result.getOrderNumber()).isEqualTo("ODN-TEST-123");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderItems()).hasSize(2);
    }

    @Test
    @DisplayName("주문 생성 시 실제 totalAmount는 단가 × 수량의 합산으로 계산된다")
    void createOrder_정상입력_totalAmount실제계산검증() {
        // given: 상품A(100,000원) × 2개, 상품B(5,000원) × 3개 주문 요청
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        CreateOrderRequestDto request = createOrderRequest(List.of(
                createItemRequest(1L, 2),
                createItemRequest(2L, 3)
        ));
        given(userRepository.findById(1L)).willReturn(Optional.of(테스트유저));
        given(productRepository.findAllById(Set.of(1L, 2L))).willReturn(List.of(상품A, 상품B));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        orderService.createOrder(1L, request);

        // then: 저장된 Order 객체의 totalAmount가 (100,000 × 2) + (5,000 × 3) = 215,000으로 계산된다
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getTotalAmount()).isEqualTo(215000);
    }

    @Test
    @DisplayName("단일 상품 주문 시 totalAmount는 단가 × 수량으로 계산된다")
    void createOrder_단일상품_totalAmount계산검증() {
        // given: 상품A(100,000원) × 3개 단일 상품 주문
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        CreateOrderRequestDto request = createOrderRequest(List.of(createItemRequest(1L, 3)));
        given(userRepository.findById(1L)).willReturn(Optional.of(테스트유저));
        given(productRepository.findAllById(Set.of(1L))).willReturn(List.of(상품A));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        orderService.createOrder(1L, request);

        // then: totalAmount가 100,000 × 3 = 300,000으로 계산된다
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getTotalAmount()).isEqualTo(300000);
    }

    @Test
    @DisplayName("동일한 상품 ID가 여러 번 들어오면 수량을 병합하여 단일 주문 항목으로 처리한다")
    void createOrder_중복상품요청_수량병합처리() {
        // given: 상품A에 대해 수량 2개, 1개로 분할된 중복 요청 인입
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        CreateOrderRequestDto request = createOrderRequest(List.of(
                createItemRequest(1L, 2),
                createItemRequest(1L, 1)
        ));
        given(userRepository.findById(1L)).willReturn(Optional.of(테스트유저));
        given(productRepository.findAllById(Set.of(1L))).willReturn(List.of(상품A));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        orderService.createOrder(1L, request);

        // then: 상품A의 주문 항목이 1개로 합쳐지고 수량이 3으로 병합되어 저장된다
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderItems()).hasSize(1);
        assertThat(orderCaptor.getValue().getOrderItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("중복 상품 수량 병합 시 totalAmount도 병합된 수량 기준으로 계산된다")
    void createOrder_중복상품요청_totalAmount병합수량기준계산() {
        // given: 상품A(100,000원)에 대해 2개 + 1개 중복 요청 (총 3개)
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        CreateOrderRequestDto request = createOrderRequest(List.of(
                createItemRequest(1L, 2),
                createItemRequest(1L, 1)
        ));
        given(userRepository.findById(1L)).willReturn(Optional.of(테스트유저));
        given(productRepository.findAllById(Set.of(1L))).willReturn(List.of(상품A));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        orderService.createOrder(1L, request);

        // then: totalAmount가 100,000 × 3 = 300,000으로 계산된다
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getTotalAmount()).isEqualTo(300000);
    }

    @Test
    @DisplayName("주문 아이템이 비어있으면 INVALID_INPUT 예외가 발생한다")
    void createOrder_빈아이템목록_INVALID_INPUT() {
        // given: orderItems가 빈 리스트인 요청
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        CreateOrderRequestDto request = createOrderRequest(List.of());
        given(userRepository.findById(1L)).willReturn(Optional.of(테스트유저));

        // when & then: 아이템 검증 단계에서 INVALID_INPUT 예외가 발생한다
        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(OrderException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("주문 요청한 상품 중 DB에 존재하지 않는 상품이 섞여있으면 ORDERITEM_NOT_FOUND 예외가 발생한다")
    void createOrder_일부상품없음_ORDERITEM_NOT_FOUND() {
        // given: DB에 존재하지 않는 상품 ID가 포함된 요청
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        CreateOrderRequestDto request = createOrderRequest(List.of(
                createItemRequest(1L, 1),
                createItemRequest(999L, 1)
        ));
        given(userRepository.findById(1L)).willReturn(Optional.of(테스트유저));
        given(productRepository.findAllById(Set.of(1L, 999L))).willReturn(List.of(상품A));

        // when & then: 조회된 상품 수와 요청 상품 수 불일치로 예외가 발생한다
        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(OrderException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDERITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 요청한 상품의 재고가 부족하면 PRODUCT_OUT_OF_STOCK 예외가 발생한다")
    void createOrder_재고부족_PRODUCT_OUT_OF_STOCK() {
        // given: 현재 재고(10개)보다 큰 수량(15개) 주문 요청
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        CreateOrderRequestDto request = createOrderRequest(List.of(createItemRequest(1L, 15)));
        given(userRepository.findById(1L)).willReturn(Optional.of(테스트유저));
        given(productRepository.findAllById(Set.of(1L))).willReturn(List.of(상품A));

        // when & then: 재고 검증 단계에서 PRODUCT_OUT_OF_STOCK 예외가 발생한다
        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(ProductException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_OUT_OF_STOCK);
    }

    @Test
    @DisplayName("판매 중이 아닌 상품을 주문하려 하면 ORDERITEM_UNAVAILABLE 예외가 발생한다")
    void createOrder_품절상품주문_ORDERITEM_UNAVAILABLE() {
        // given: SOLD_OUT 상태의 상품에 대한 주문 요청
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        CreateOrderRequestDto request = createOrderRequest(List.of(createItemRequest(3L, 1)));
        given(userRepository.findById(1L)).willReturn(Optional.of(테스트유저));
        given(productRepository.findAllById(Set.of(3L))).willReturn(List.of(품절상품));

        // when & then: 상품 상태 검증 단계에서 ORDERITEM_UNAVAILABLE 예외가 발생한다
        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(OrderException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDERITEM_UNAVAILABLE);
    }

    @Test
    @DisplayName("주문 목록 조회 시 해당 유저의 주문이 최신순으로 반환된다")
    void getAllOrderList_주문존재_목록반환() {
        // given: 특정 유저의 주문 2건이 최신순으로 조회되는 상황
        ReflectionTestUtils.setField(테스트유저, "id", 1L);

        Order order1 = createInstance(Order.class);
        ReflectionTestUtils.setField(order1, "id", 1L);
        ReflectionTestUtils.setField(order1, "orderNumber", "ODN-001");
        ReflectionTestUtils.setField(order1, "totalAmount", 100000);
        ReflectionTestUtils.setField(order1, "orderStatus", com.example.a3sproject.domain.order.enums.OrderStatus.PENDING);
        ReflectionTestUtils.setField(order1, "user", 테스트유저);
        ReflectionTestUtils.setField(order1, "orderItems", new java.util.ArrayList<>());
        ReflectionTestUtils.setField(order1, "usedPointAmount", 0);
        ReflectionTestUtils.setField(order1, "finalAmount", 100000);

        Order order2 = createInstance(Order.class);
        ReflectionTestUtils.setField(order2, "id", 2L);
        ReflectionTestUtils.setField(order2, "orderNumber", "ODN-002");
        ReflectionTestUtils.setField(order2, "totalAmount", 50000);
        ReflectionTestUtils.setField(order2, "orderStatus", com.example.a3sproject.domain.order.enums.OrderStatus.PENDING);
        ReflectionTestUtils.setField(order2, "user", 테스트유저);
        ReflectionTestUtils.setField(order2, "orderItems", new java.util.ArrayList<>());
        ReflectionTestUtils.setField(order2, "usedPointAmount", 0);
        ReflectionTestUtils.setField(order2, "finalAmount", 50000);

        given(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(order2, order1));
        // ↓ findByOrder → findByOrderIn으로 교체
        given(paymentRepository.findByOrderIn(List.of(order2, order1))).willReturn(List.of());

        // when
        List<GetOrderListResponseDto> result = orderService.getAllOrderList(1L);

        // then: 2건의 주문이 반환되며 최신 주문이 첫 번째로 위치한다
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderNumber()).isEqualTo("ODN-002");
        assertThat(result.get(1).getOrderNumber()).isEqualTo("ODN-001");
    }

    @Test
    @DisplayName("주문이 없는 유저가 목록 조회 시 빈 리스트를 반환한다")
    void getAllOrderList_주문없음_빈리스트반환() {
        // given: 해당 유저의 주문이 전혀 없는 상황
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        given(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

        // when
        List<GetOrderListResponseDto> result = orderService.getAllOrderList(1L);

        // then: 예외 없이 빈 리스트가 반환된다
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("주문 목록 조회 시 결제 정보가 없는 주문도 목록에 포함된다")
    void getAllOrderList_결제정보없는주문_목록에포함() {
        // given: 결제가 아직 완료되지 않은 PENDING 상태의 주문 존재
        ReflectionTestUtils.setField(테스트유저, "id", 1L);

        Order pendingOrder = createInstance(Order.class);
        ReflectionTestUtils.setField(pendingOrder, "id", 10L);
        ReflectionTestUtils.setField(pendingOrder, "orderNumber", "ODN-PENDING");
        ReflectionTestUtils.setField(pendingOrder, "totalAmount", 100000);
        ReflectionTestUtils.setField(pendingOrder, "orderStatus", com.example.a3sproject.domain.order.enums.OrderStatus.PENDING);
        ReflectionTestUtils.setField(pendingOrder, "user", 테스트유저);
        ReflectionTestUtils.setField(pendingOrder, "orderItems", new java.util.ArrayList<>());
        ReflectionTestUtils.setField(pendingOrder, "usedPointAmount", 0);
        ReflectionTestUtils.setField(pendingOrder, "finalAmount", 100000);

        given(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(pendingOrder));
        // ↓ findByOrder → findByOrderIn으로 교체, 결제 없는 케이스이므로 빈 리스트 반환
        given(paymentRepository.findByOrderIn(List.of(pendingOrder))).willReturn(List.of());

        // when
        List<GetOrderListResponseDto> result = orderService.getAllOrderList(1L);

        // then: 결제 정보가 없어도 주문 목록에 정상 포함되며 결제 상태는 null이다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderNumber()).isEqualTo("ODN-PENDING");
        assertThat(result.get(0).getPaymentStatus()).isNull();
    }

    @Test
    @DisplayName("주문 상세 조회 시 연관된 결제 정보와 적립 포인트 합산값이 반환된다")
    void getOrderDetail_정상조회_연관데이터포함반환() {
        // given: 주문, 결제, 포인트 적립 내역 2건이 존재하는 상황
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        Order order = createInstance(Order.class);
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "orderNumber", "ODN-12345");
        ReflectionTestUtils.setField(order, "totalAmount", 50000);
        ReflectionTestUtils.setField(order, "orderStatus", com.example.a3sproject.domain.order.enums.OrderStatus.PENDING);
        ReflectionTestUtils.setField(order, "user", 테스트유저);
        ReflectionTestUtils.setField(order, "orderItems", new java.util.ArrayList<>());
        ReflectionTestUtils.setField(order, "usedPointAmount", 0);
        ReflectionTestUtils.setField(order, "finalAmount", 50000);

        Payment payment = createInstance(Payment.class);
        ReflectionTestUtils.setField(payment, "order", order);
        ReflectionTestUtils.setField(payment, "paidAmount", 50000);
        ReflectionTestUtils.setField(payment, "paidStatus", PaidStatus.SUCCESS);

        PointTransaction pt1 = createInstance(PointTransaction.class);
        ReflectionTestUtils.setField(pt1, "points", 500);
        ReflectionTestUtils.setField(pt1, "type", PointTransactionType.EARN);

        PointTransaction pt2 = createInstance(PointTransaction.class);
        ReflectionTestUtils.setField(pt2, "points", 200);
        ReflectionTestUtils.setField(pt2, "type", PointTransactionType.EARN);

        given(orderRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder(order)).willReturn(Optional.of(payment));
        given(pointRepository.findByUserIdAndOrderIdAndTypeIn(1L, 1L, List.of(PointTransactionType.EARN, PointTransactionType.CANCEL)))
                .willReturn(List.of(pt1, pt2));

        // when
        GetOrderDetailResponseDto result = orderService.getOrderDetail(1L, 1L);

        // then: 주문 기본 정보, 결제 금액, 포인트 합산값(500 + 200 = 700)이 모두 반환된다
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getOrderNumber()).isEqualTo("ODN-12345");
        assertThat(result.getTotalAmount()).isEqualTo(50000);
        assertThat(result.getEarnedPoints()).isEqualTo(700);
        assertThat(result.getPaidAmount()).isEqualTo(50000);
    }

    @Test
    @DisplayName("환불로 인해 CANCEL 타입 포인트가 존재하면 적립 포인트에서 차감되어 합산된다")
    void getOrderDetail_포인트취소존재_적립포인트차감합산() {
        // given: EARN 500점, CANCEL -200점 포인트 거래 내역이 모두 존재하는 상황
        ReflectionTestUtils.setField(테스트유저, "id", 1L);
        Order order = createInstance(Order.class);
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "orderNumber", "ODN-cancel");
        ReflectionTestUtils.setField(order, "totalAmount", 100000);
        ReflectionTestUtils.setField(order, "orderStatus", com.example.a3sproject.domain.order.enums.OrderStatus.PENDING);
        ReflectionTestUtils.setField(order, "user", 테스트유저);
        ReflectionTestUtils.setField(order, "orderItems", new java.util.ArrayList<>());
        ReflectionTestUtils.setField(order, "usedPointAmount", 0);
        ReflectionTestUtils.setField(order, "finalAmount", 100000);

        PointTransaction earn = createInstance(PointTransaction.class);
        ReflectionTestUtils.setField(earn, "points", 500);
        ReflectionTestUtils.setField(earn, "type", PointTransactionType.EARN);

        PointTransaction cancel = createInstance(PointTransaction.class);
        ReflectionTestUtils.setField(cancel, "points", -200);
        ReflectionTestUtils.setField(cancel, "type", PointTransactionType.CANCEL);

        given(orderRepository.findByIdAndUser_Id(2L, 1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder(order)).willReturn(Optional.empty());
        given(pointRepository.findByUserIdAndOrderIdAndTypeIn(1L, 2L, List.of(PointTransactionType.EARN, PointTransactionType.CANCEL)))
                .willReturn(List.of(earn, cancel));

        // when
        GetOrderDetailResponseDto result = orderService.getOrderDetail(1L, 2L);

        // then: earnedPoints가 500 + (-200) = 300으로 합산된다
        assertThat(result.getEarnedPoints()).isEqualTo(300);
    }

    @Test
    @DisplayName("본인의 주문이 아니거나 존재하지 않는 주문을 조회하면 ORDER_NOT_FOUND 예외가 발생한다")
    void getOrderDetail_권한없음또는존재하지않음_ORDER_NOT_FOUND() {
        // given: 타인의 주문이거나 존재하지 않는 주문 ID 조회
        given(orderRepository.findByIdAndUser_Id(999L, 1L)).willReturn(Optional.empty());

        // when & then: ORDER_NOT_FOUND 에러코드를 가진 OrderException이 발생한다
        assertThatThrownBy(() -> orderService.getOrderDetail(1L, 999L))
                .isInstanceOf(OrderException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }
}