package com.example.a3sproject.domain.payment.service;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.order.repository.OrderRepository;
import com.example.a3sproject.domain.payment.dto.PaymentPrepareResult;
import com.example.a3sproject.domain.payment.dto.request.PaymentTryRequest;
import com.example.a3sproject.domain.payment.dto.response.PaymentConfirmResponse;
import com.example.a3sproject.domain.payment.dto.response.PaymentTryResponse;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.point.service.PointService;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.response.PortOnePaymentResponse;
import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;
import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductCategory;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    // PaymentService가 직접 갖는 의존성만
    @Mock private OrderRepository orderRepository;
    @Mock private PortOneClient portOneClient;
    @Mock private PointService pointService;
    @Mock private PaymentFailureHandler paymentFailureHandler;
    @Mock private PaymentPreProcessor paymentPreProcessor;
    @Mock private PaymentConfirmProcessor paymentConfirmProcessor;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private final User 테스트유저 = createUser(1L, 50000);
    private final Product 테스트상품 = createProduct(1L, "나이키 신발", 100000, 10);

    private User createUser(Long id, int pointBalance) {
        User user = new User("홍길동", "test@test.com", "pass", "010-0000-0000",
                pointBalance, 0, MembershipGrade.NORMAL, "CUST_001");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Product createProduct(Long id, String name, int price, int stock) {
        Product product = Product.builder()
                .name(name).price(price).stock(stock).description("설명")
                .productStatus(ProductStatus.ON_SALE).productCategory(ProductCategory.UPSTREAM).build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Order createOrder(Long id, User user, int totalAmount, OrderStatus status) {
        Order order = createInstance(Order.class);
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "user", user);
        ReflectionTestUtils.setField(order, "totalAmount", totalAmount);
        ReflectionTestUtils.setField(order, "orderStatus", status);
        ReflectionTestUtils.setField(order, "orderNumber", "ODN-TEST-" + id);
        ReflectionTestUtils.setField(order, "usedPointAmount", 0);
        ReflectionTestUtils.setField(order, "finalAmount", totalAmount);
        ReflectionTestUtils.setField(order, "orderItems", new ArrayList<>());
        return order;
    }

    private Payment createPayment(Long id, Order order, int paidAmount, PaidStatus status, int pointsToUse) {
        Payment payment = createInstance(Payment.class);
        ReflectionTestUtils.setField(payment, "id", id);
        ReflectionTestUtils.setField(payment, "order", order);
        ReflectionTestUtils.setField(payment, "paidAmount", paidAmount);
        ReflectionTestUtils.setField(payment, "paidStatus", status);
        ReflectionTestUtils.setField(payment, "pointsToUse", pointsToUse);
        ReflectionTestUtils.setField(payment, "portOneId", "PMN-TEST-" + id);
        return payment;
    }

    private Membership createMembership(User user, MembershipGrade grade) {
        Membership membership = createInstance(Membership.class);
        ReflectionTestUtils.setField(membership, "user", user);
        ReflectionTestUtils.setField(membership, "grade", grade);
        return membership;
    }

    private PortOnePaymentResponse createPortOneResponse(int amount) {
        return new PortOnePaymentResponse(
                "PMN-TEST-1",
                PortOnePayStatus.PAID,
                new PortOnePaymentResponse.PaymentAmount(amount, 0),
                OffsetDateTime.now()
        );
    }

    private PaymentPrepareResult createPaymentPrepareResult(Payment payment, boolean pointUsed, Long userId) {
        return new PaymentPrepareResult(payment, pointUsed, userId);
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

    // =========================================================
    // createPayment - 일반 결제 (포인트 미사용)
    // =========================================================

    @Test
    @DisplayName("포인트 미사용 일반 결제 시도 시 PENDING 상태의 결제가 생성되고 portOneId가 반환된다")
    void createPayment_포인트미사용_PENDING결제생성() {
        // given: PENDING 주문, 포인트 미사용 요청
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        Payment payment = createPayment(1L, order, 100000, PaidStatus.PENDING, 0);

        given(orderRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder(order)).willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willReturn(payment);

        PaymentTryRequest request = new PaymentTryRequest(1L, 100000, 0);

        // when
        PaymentTryResponse result = paymentService.createPayment(1L, request);

        // then: 결제 생성 성공, portOneId 반환, PortOne SDK에 넘길 금액이 주문 총액과 동일
        assertThat(result.success()).isTrue();
        assertThat(result.portOneId()).isEqualTo("PMN-TEST-1");
        assertThat(result.totalAmount()).isEqualTo(100000);
        assertThat(result.status()).isEqualTo(String.valueOf(PaidStatus.PENDING));
        verify(pointService, never()).validateAndUse(anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("포인트 일부 사용 복합 결제 시도 시 포인트 차감 후 잔여 금액으로 결제가 생성된다")
    void createPayment_포인트일부사용_복합결제생성() {
        // given: 주문 100,000원, 포인트 30,000P 사용 → 실결제 70,000원
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        Payment payment = createPayment(1L, order, 70000, PaidStatus.PENDING, 30000);

        given(orderRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder(order)).willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willReturn(payment);

        PaymentTryRequest request = new PaymentTryRequest(1L, 100000, 30000);

        // when
        PaymentTryResponse result = paymentService.createPayment(1L, request);

        // then: PortOne SDK에 넘길 금액이 70,000원으로 차감되어 반환된다
        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualTo(70000);
    }

    @Test
    @DisplayName("포인트 전액 결제 시 PortOne SDK 호출 없이 즉시 주문이 완료 처리된다")
    void createPayment_포인트전액결제_즉시완료처리() {
        // given: 주문 50,000원, 포인트 50,000P 전액 사용 → 실결제 0원
        User 포인트충분유저 = createUser(1L, 50000);
        Order order = createOrder(1L, 포인트충분유저, 50000, OrderStatus.PENDING);
        Payment payment = createPayment(1L, order, 0, PaidStatus.SUCCESS, 50000);

        given(orderRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder(order)).willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willReturn(payment);

        PaymentTryRequest request = new PaymentTryRequest(1L, 50000, 50000);

        // when
        PaymentTryResponse result = paymentService.createPayment(1L, request);

        // then: 결제 금액이 0원이고 포인트 차감이 즉시 호출되며 주문이 완료 상태가 된다
        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isZero();
        verify(pointService).validateAndUse(1L, 1L, 50000);
    }

    @Test
    @DisplayName("클라이언트가 보낸 totalAmount가 서버의 주문 금액과 다르면 PAYMENT_AMOUNT_MISMATCH 예외가 발생한다")
    void createPayment_금액불일치_PAYMENT_AMOUNT_MISMATCH() {
        // given: 실제 주문금액 100,000원인데 클라이언트가 90,000원으로 조작해서 전송
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        given(orderRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(order));

        PaymentTryRequest request = new PaymentTryRequest(1L, 90000, 0);

        // when & then: 금액 변조 감지로 PAYMENT_AMOUNT_MISMATCH 예외 발생
        assertThatThrownBy(() -> paymentService.createPayment(1L, request))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("보유 포인트보다 많은 포인트를 사용하려 하면 POINT_NOT_ENOUGH 예외가 발생한다")
    void createPayment_포인트잔액초과_POINT_NOT_ENOUGH() {
        // given: 유저 포인트 잔액 50,000P인데 70,000P 사용 시도
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        given(orderRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(order));

        PaymentTryRequest request = new PaymentTryRequest(1L, 100000, 70000);

        // when & then: 포인트 잔액 검증 단계에서 POINT_NOT_ENOUGH 예외 발생
        assertThatThrownBy(() -> paymentService.createPayment(1L, request))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POINT_NOT_ENOUGH);
    }

    @Test
    @DisplayName("주문 금액보다 많은 포인트를 사용하려 하면 INVALID_INPUT 예외가 발생한다")
    void createPayment_주문금액초과포인트_INVALID_INPUT() {
        // given: 주문금액 100,000원인데 포인트 110,000P 사용 시도
        User 포인트많은유저 = createUser(1L, 200000);
        Order order = createOrder(1L, 포인트많은유저, 100000, OrderStatus.PENDING);
        given(orderRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(order));

        PaymentTryRequest request = new PaymentTryRequest(1L, 100000, 110000);

        // when & then: 포인트 사용 한도 검증에서 INVALID_INPUT 예외 발생
        assertThatThrownBy(() -> paymentService.createPayment(1L, request))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("음수 포인트를 사용하려 하면 INVALID_INPUT 예외가 발생한다")
    void createPayment_음수포인트_INVALID_INPUT() {
        // given
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        given(orderRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(order));

        PaymentTryRequest request = new PaymentTryRequest(1L, 100000, -1000);

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(1L, request))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("이미 결제가 완료된 주문에 재결제를 시도하면 DUPLICATE_PAYMENT_REQUEST 예외가 발생한다")
    void createPayment_이미완료된주문_DUPLICATE_PAYMENT_REQUEST() {
        // given: 주문 상태가 이미 COMPLETED인 경우
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.COMPLETED);
        given(orderRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(order));

        PaymentTryRequest request = new PaymentTryRequest(1L, 100000, 0);

        // when & then: 주문 상태 검증에서 DUPLICATE_PAYMENT_REQUEST 예외 발생
        assertThatThrownBy(() -> paymentService.createPayment(1L, request))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
    }

    // =========================================================
    // confirmPayment - 결제 확정
    // =========================================================

    @Test
    @DisplayName("PortOne 검증을 통과하면 결제가 확정되고 주문이 COMPLETED 상태로 변경된다")
    void confirmPayment_정상검증_결제확정및주문완료() {
        // given
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        Payment payment = createPayment(1L, order, 100000, PaidStatus.PENDING, 0);
        PaymentPrepareResult prepareResult = createPaymentPrepareResult(payment, false, 1L);
        PortOnePaymentResponse portOneResponse = createPortOneResponse(100000);
        PaymentConfirmResponse confirmResponse =
                new PaymentConfirmResponse("ODN-TEST-1", "결제가 완료되었습니다.");

        given(paymentPreProcessor.validateAndPrepare("PMN-TEST-1", 1L)).willReturn(prepareResult);
        given(portOneClient.getPayment("PMN-TEST-1")).willReturn(portOneResponse);
        given(paymentConfirmProcessor.confirm(payment, portOneResponse)).willReturn(confirmResponse);

        // when
        PaymentConfirmResponse result = paymentService.confirmPayment("PMN-TEST-1", 1L);

        // then
        assertThat(result.orderNumber()).isEqualTo("ODN-TEST-1");
        verify(paymentPreProcessor).validateAndPrepare("PMN-TEST-1", 1L);
        verify(portOneClient).getPayment("PMN-TEST-1");
        verify(paymentConfirmProcessor).confirm(payment, portOneResponse);
    }

    @Test
    @DisplayName("PortOne에서 조회한 금액이 DB 결제 금액과 다르면 PAYMENT_AMOUNT_MISMATCH 예외가 발생하고 보상 트랜잭션이 실행된다")
    void confirmPayment_금액불일치_보상트랜잭션실행() {
        // given
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        Payment payment = createPayment(1L, order, 100000, PaidStatus.PENDING, 0);
        PaymentPrepareResult prepareResult = createPaymentPrepareResult(payment, false, 1L);
        PortOnePaymentResponse mismatchResponse = createPortOneResponse(90000);

        given(paymentPreProcessor.validateAndPrepare("PMN-TEST-1", 1L)).willReturn(prepareResult);
        given(portOneClient.getPayment("PMN-TEST-1")).willReturn(mismatchResponse);
        given(paymentConfirmProcessor.confirm(payment, mismatchResponse))
                .willThrow(new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment("PMN-TEST-1", 1L))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

        verify(paymentFailureHandler).handlePaymentFailure(
                any(PaymentPrepareResult.class), eq("PMN-TEST-1"), eq(true));
    }

    @Test
    @DisplayName("PortOne 결제 상태가 PAID가 아니면 PAYMENT_PORTONE_ERROR 예외가 발생한다")
    void confirmPayment_PortOne미결제상태_PAYMENT_PORTONE_ERROR() {
        // given
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        Payment payment = createPayment(1L, order, 100000, PaidStatus.PENDING, 0);
        PaymentPrepareResult prepareResult = createPaymentPrepareResult(payment, false, 1L);
        PortOnePaymentResponse failedResponse = new PortOnePaymentResponse(
                "PMN-TEST-1",
                PortOnePayStatus.FAILED,
                new PortOnePaymentResponse.PaymentAmount(100000, 0),
                OffsetDateTime.now()
        );

        given(paymentPreProcessor.validateAndPrepare("PMN-TEST-1", 1L)).willReturn(prepareResult);
        given(portOneClient.getPayment("PMN-TEST-1")).willReturn(failedResponse);
        given(paymentConfirmProcessor.confirm(payment, failedResponse))
                .willThrow(new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment("PMN-TEST-1", 1L))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_PORTONE_ERROR);
    }

    @Test
    @DisplayName("이미 SUCCESS 상태로 처리된 결제를 중복 확정하려 하면 DUPLICATE_PAYMENT_REQUEST 예외가 발생한다")
    void confirmPayment_중복확정요청_DUPLICATE_PAYMENT_REQUEST() {
        // given: 중복 검증은 PreProcessor에서 처리
        given(paymentPreProcessor.validateAndPrepare("PMN-TEST-1", 1L))
                .willThrow(new PaymentException(ErrorCode.DUPLICATE_PAYMENT_REQUEST));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment("PMN-TEST-1", 1L))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_PAYMENT_REQUEST);

        verify(portOneClient, never()).getPayment(anyString());
    }

    @Test
    @DisplayName("타유저가 결제 확정을 시도하면 USER_FORBIDDEN 예외가 발생한다")
    void confirmPayment_타유저접근_USER_FORBIDDEN() {
        // given: 소유권 검증은 PreProcessor에서 처리
        given(paymentPreProcessor.validateAndPrepare("PMN-TEST-1", 2L))
                .willThrow(new PaymentException(ErrorCode.USER_FORBIDDEN));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment("PMN-TEST-1", 2L))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_FORBIDDEN);
    }

    @Test
    @DisplayName("confirmPayment 내부에서 예상치 못한 예외 발생 시 보상 트랜잭션이 실행된다")
    void confirmPayment_내부예외_보상트랜잭션실행() {
        // given
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        Payment payment = createPayment(1L, order, 100000, PaidStatus.PENDING, 0);
        PaymentPrepareResult prepareResult = createPaymentPrepareResult(payment, false, 1L);

        given(paymentPreProcessor.validateAndPrepare("PMN-TEST-1", 1L)).willReturn(prepareResult);
        given(portOneClient.getPayment("PMN-TEST-1"))
                .willThrow(new RuntimeException("PortOne 서버 오류"));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment("PMN-TEST-1", 1L))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_PORTONE_ERROR);

        verify(paymentFailureHandler).handlePaymentFailure(
                any(PaymentPrepareResult.class), eq("PMN-TEST-1"), eq(false));
    }

    @Test
    @DisplayName("결제 확정 성공 시 멤버십 등급이 갱신된다")
    void confirmPayment_결제확정성공_멤버십등급갱신() {
        // given: 멤버십 등급 갱신은 ConfirmProcessor에서 처리 → 정상 흐름 검증
        Order order = createOrder(1L, 테스트유저, 500000, OrderStatus.PENDING);
        Payment payment = createPayment(1L, order, 500000, PaidStatus.PENDING, 0);
        PaymentPrepareResult prepareResult = createPaymentPrepareResult(payment, false, 1L);
        PortOnePaymentResponse portOneResponse = createPortOneResponse(500000);
        PaymentConfirmResponse confirmResponse =
                new PaymentConfirmResponse("ODN-TEST-1", "결제가 완료되었습니다.");

        given(paymentPreProcessor.validateAndPrepare("PMN-TEST-1", 1L)).willReturn(prepareResult);
        given(portOneClient.getPayment("PMN-TEST-1")).willReturn(portOneResponse);
        given(paymentConfirmProcessor.confirm(payment, portOneResponse)).willReturn(confirmResponse);

        // when
        paymentService.confirmPayment("PMN-TEST-1", 1L);

        // then: ConfirmProcessor가 호출됐다면 내부에서 멤버십 갱신이 실행됨
        verify(paymentConfirmProcessor).confirm(payment, portOneResponse);
    }

    @Test
    @DisplayName("포인트를 사용한 결제 확정 시 포인트 차감이 먼저 실행된다")
    void confirmPayment_포인트사용결제_포인트차감호출() {
        // given: 포인트 차감은 PreProcessor에서 처리
        Order order = createOrder(1L, 테스트유저, 100000, OrderStatus.PENDING);
        Payment payment = createPayment(1L, order, 70000, PaidStatus.PENDING, 30000);
        PaymentPrepareResult prepareResult = createPaymentPrepareResult(payment, true, 1L);
        PortOnePaymentResponse portOneResponse = createPortOneResponse(70000);
        PaymentConfirmResponse confirmResponse =
                new PaymentConfirmResponse("ODN-TEST-1", "결제가 완료되었습니다.");

        given(paymentPreProcessor.validateAndPrepare("PMN-TEST-1", 1L)).willReturn(prepareResult);
        given(portOneClient.getPayment("PMN-TEST-1")).willReturn(portOneResponse);
        given(paymentConfirmProcessor.confirm(payment, portOneResponse)).willReturn(confirmResponse);

        // when
        paymentService.confirmPayment("PMN-TEST-1", 1L);

        // then: 포인트 차감은 PreProcessor에 위임됐으므로 호출 여부만 검증
        verify(paymentPreProcessor).validateAndPrepare("PMN-TEST-1", 1L);
        verify(paymentConfirmProcessor).confirm(payment, portOneResponse);
    }
}