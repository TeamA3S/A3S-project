package com.example.a3sproject.domain.refunds.service;

import com.example.a3sproject.config.PortOneProperties;
import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import com.example.a3sproject.domain.point.service.PointService;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentRequest;
import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentResponse;
import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductCategory;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.refunds.dto.request.RefundRequestDto;
import com.example.a3sproject.domain.refunds.dto.response.RefundResponseDto;
import com.example.a3sproject.domain.refunds.enums.RefundStatus;
import com.example.a3sproject.domain.refunds.repository.RefundRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.RefundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock private RefundRepository refundRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PortOneClient portOneClient;
    @Mock private RefundRecordService refundRecordService;
    @Mock private PointService pointService;
    @Mock private PointRepository pointRepository;
    @Mock private PortOneProperties portOneProperties;
    @Mock private MembershipRepository membershipRepository;

    @InjectMocks
    private RefundService refundService;

    private User createUser(Long id, int pointBalance, int totalPaymentAmount) {
        User user = new User("홍길동", "test@test.com", "pass", "010-0000-0000",
                pointBalance, totalPaymentAmount, MembershipGrade.NORMAL, "CUST_001");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Product createProduct(Long id, int stock) {
        Product product = Product.builder()
                .name("테스트 상품").price(100000).stock(stock).description("설명")
                .productStatus(ProductStatus.ON_SALE).productCategory(ProductCategory.UPSTREAM).build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Order createOrder(Long id, User user, OrderStatus status, int totalAmount, int usedPointAmount) {
        Order order = createInstance(Order.class);
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "user", user);
        ReflectionTestUtils.setField(order, "orderStatus", status);
        ReflectionTestUtils.setField(order, "totalAmount", totalAmount);
        ReflectionTestUtils.setField(order, "usedPointAmount", usedPointAmount);
        ReflectionTestUtils.setField(order, "finalAmount", totalAmount - usedPointAmount);
        ReflectionTestUtils.setField(order, "orderNumber", "ODN-TEST-" + id);
        ReflectionTestUtils.setField(order, "orderItems", new java.util.ArrayList<>());
        return order;
    }

    private Payment createPayment(Long id, Order order, PaidStatus status, int paidAmount) {
        Payment payment = createInstance(Payment.class);
        ReflectionTestUtils.setField(payment, "id", id);
        ReflectionTestUtils.setField(payment, "order", order);
        ReflectionTestUtils.setField(payment, "paidStatus", status);
        ReflectionTestUtils.setField(payment, "paidAmount", paidAmount);
        ReflectionTestUtils.setField(payment, "portOneId", "PMN-TEST-" + id);
        ReflectionTestUtils.setField(payment, "pointsToUse", 0);
        return payment;
    }

    private Membership createMembership(User user, MembershipGrade grade) {
        Membership membership = createInstance(Membership.class);
        ReflectionTestUtils.setField(membership, "user", user);
        ReflectionTestUtils.setField(membership, "grade", grade);
        return membership;
    }

    private PortOneCancelPaymentResponse createCancelResponse() {
        PortOneCancelPaymentResponse.Cancellation cancellation =
                new PortOneCancelPaymentResponse.Cancellation("CANCELLED", 100000, OffsetDateTime.now());
        return new PortOneCancelPaymentResponse(cancellation);
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

    private RefundRequestDto createRefundRequest(String reason) {
        RefundRequestDto dto = new RefundRequestDto();
        ReflectionTestUtils.setField(dto, "reason", reason);
        return dto;
    }

    @Test
    @DisplayName("정상적인 환불 요청 시 PortOne 취소 API가 호출되고 환불 이력이 저장된다")
    void refundPayment_정상요청_PortOne취소및환불이력저장() {
        // given: COMPLETED 주문, SUCCESS 결제, 환불 이력 없음
        User user = createUser(1L, 0, 100000);
        Order order = createOrder(1L, user, OrderStatus.COMPLETED, 100000, 0);
        Payment payment = createPayment(1L, order, PaidStatus.SUCCESS, 100000);
        Membership membership = createMembership(user, MembershipGrade.NORMAL);

        PortOneProperties.Store store = new PortOneProperties.Store();
        ReflectionTestUtils.setField(store, "id", "STORE_001");

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(false);
        given(portOneProperties.getStore()).willReturn(store);
        given(portOneClient.cancelPayment(anyString(), any(PortOneCancelPaymentRequest.class)))
                .willReturn(createCancelResponse());
        given(pointRepository.findByOrderIdAndType(1L, PointTransactionType.EARN)).willReturn(List.of());
        given(membershipRepository.findWithLockByUser(user)).willReturn(Optional.of(membership));

        // when
        RefundResponseDto result = refundService.refundPayment(1L, "PMN-TEST-1", createRefundRequest("단순 변심"));

        // then: 환불 성공, PortOne 취소 호출, 환불 이력 저장, 주문 상태 REFUNDED
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderNumber()).isEqualTo("ODN-TEST-1");
        verify(portOneClient).cancelPayment(anyString(), any(PortOneCancelPaymentRequest.class));
        verify(refundRecordService).saveSuccessRefund(any(), anyString(), any());
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    @DisplayName("환불 성공 시 총 결제금액이 차감되고 멤버십 등급이 재계산된다")
    void refundPayment_환불성공_총결제금액차감및등급재계산() {
        // given: 총 결제금액 500,000원 VVIP 유저, 100,000원 환불 요청
        User user = createUser(1L, 0, 500000);
        Order order = createOrder(1L, user, OrderStatus.COMPLETED, 100000, 0);
        Payment payment = createPayment(1L, order, PaidStatus.SUCCESS, 100000);
        Membership membership = createMembership(user, MembershipGrade.VVIP);

        PortOneProperties.Store store = new PortOneProperties.Store();
        ReflectionTestUtils.setField(store, "id", "STORE_001");

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(false);
        given(portOneProperties.getStore()).willReturn(store);
        given(portOneClient.cancelPayment(anyString(), any(PortOneCancelPaymentRequest.class)))
                .willReturn(createCancelResponse());
        given(pointRepository.findByOrderIdAndType(1L, PointTransactionType.EARN)).willReturn(List.of());
        given(membershipRepository.findWithLockByUser(user)).willReturn(Optional.of(membership));

        // when
        refundService.refundPayment(1L, "PMN-TEST-1", createRefundRequest("변심"));

        // then: 총 결제금액 500,000 - 100,000 = 400,000원으로 차감
        assertThat(user.getTotalPaymentAmount()).isEqualTo(400000);
        // 400,000원 기준 등급 재계산 → VIP(300,000 이상 500,000 미만)로 다운그레이드
        assertThat(membership.getGrade()).isEqualTo(MembershipGrade.VIP);
    }

    @Test
    @DisplayName("포인트를 사용한 주문 환불 시 사용했던 포인트가 복구된다")
    void refundPayment_포인트사용주문_포인트복구() {
        // given: 30,000P 사용한 주문 환불 요청
        User user = createUser(1L, 0, 70000);
        Order order = createOrder(1L, user, OrderStatus.COMPLETED, 100000, 30000);
        Payment payment = createPayment(1L, order, PaidStatus.SUCCESS, 70000);
        Membership membership = createMembership(user, MembershipGrade.NORMAL);

        PortOneProperties.Store store = new PortOneProperties.Store();
        ReflectionTestUtils.setField(store, "id", "STORE_001");

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(false);
        given(portOneProperties.getStore()).willReturn(store);
        given(portOneClient.cancelPayment(anyString(), any(PortOneCancelPaymentRequest.class)))
                .willReturn(createCancelResponse());
        given(pointRepository.findByOrderIdAndType(1L, PointTransactionType.EARN)).willReturn(List.of());
        given(membershipRepository.findWithLockByUser(user)).willReturn(Optional.of(membership));

        // when
        refundService.refundPayment(1L, "PMN-TEST-1", createRefundRequest("변심"));

        // then: 사용했던 30,000P가 복구 호출된다
        verify(pointService).restorePoint(1L, 1L, 30000);
    }

    @Test
    @DisplayName("환불 시 결제로 적립된 포인트가 취소된다")
    void refundPayment_적립포인트존재_적립포인트취소() {
        // given: 결제로 1,000P가 적립된 주문 환불
        User user = createUser(1L, 1000, 100000);
        Order order = createOrder(1L, user, OrderStatus.COMPLETED, 100000, 0);
        Payment payment = createPayment(1L, order, PaidStatus.SUCCESS, 100000);
        Membership membership = createMembership(user, MembershipGrade.NORMAL);

        PointTransaction earnTx = PointTransaction.of(1L, 1L, 1000, 1000,
                PointTransactionType.EARN, 1000, null);

        PortOneProperties.Store store = new PortOneProperties.Store();
        ReflectionTestUtils.setField(store, "id", "STORE_001");

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(false);
        given(portOneProperties.getStore()).willReturn(store);
        given(portOneClient.cancelPayment(anyString(), any(PortOneCancelPaymentRequest.class)))
                .willReturn(createCancelResponse());
        given(pointRepository.findByOrderIdAndType(1L, PointTransactionType.EARN))
                .willReturn(List.of(earnTx));
        given(membershipRepository.findWithLockByUser(user)).willReturn(Optional.of(membership));

        // when
        refundService.refundPayment(1L, "PMN-TEST-1", createRefundRequest("변심"));

        // then: 적립됐던 1,000P 취소 호출된다
        verify(pointService).cancelEarnedPoint(1L, 1L, 1000);
    }

    @Test
    @DisplayName("환불 성공 시 주문 상품의 재고가 복구된다")
    void refundPayment_환불성공_재고복구() {
        // given: 상품 재고 5개에서 3개 주문했던 주문 환불
        User user = createUser(1L, 0, 100000);
        Product product = createProduct(1L, 5);
        Order order = createOrder(1L, user, OrderStatus.COMPLETED, 100000, 0);
        OrderItem orderItem = OrderItem.createOrderItem(product, 3);
        ReflectionTestUtils.setField(orderItem, "order", order);
        ReflectionTestUtils.setField(order, "orderItems", List.of(orderItem));
        Payment payment = createPayment(1L, order, PaidStatus.SUCCESS, 100000);
        Membership membership = createMembership(user, MembershipGrade.NORMAL);

        PortOneProperties.Store store = new PortOneProperties.Store();
        ReflectionTestUtils.setField(store, "id", "STORE_001");

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(false);
        given(portOneProperties.getStore()).willReturn(store);
        given(portOneClient.cancelPayment(anyString(), any(PortOneCancelPaymentRequest.class)))
                .willReturn(createCancelResponse());
        given(pointRepository.findByOrderIdAndType(1L, PointTransactionType.EARN)).willReturn(List.of());
        given(membershipRepository.findWithLockByUser(user)).willReturn(Optional.of(membership));

        // when
        refundService.refundPayment(1L, "PMN-TEST-1", createRefundRequest("변심"));

        // then: 주문 수량(3개)만큼 재고가 복구되어 5 + 3 = 8이 된다
        assertThat(product.getStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("포인트 전액 결제 주문(paidAmount=0) 환불 시 PortOne 취소 API가 호출되지 않는다")
    void refundPayment_포인트전액결제_PortOne취소미호출() {
        // given: 포인트 전액 결제로 paidAmount가 0인 주문 환불
        User user = createUser(1L, 0, 0);
        Order order = createOrder(1L, user, OrderStatus.COMPLETED, 50000, 50000);
        Payment payment = createPayment(1L, order, PaidStatus.SUCCESS, 0);
        Membership membership = createMembership(user, MembershipGrade.NORMAL);

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(false);
        given(pointRepository.findByOrderIdAndType(1L, PointTransactionType.EARN)).willReturn(List.of());
        given(membershipRepository.findWithLockByUser(user)).willReturn(Optional.of(membership));

        // when
        refundService.refundPayment(1L, "PMN-TEST-1", createRefundRequest("변심"));

        // then: paidAmount가 0이므로 PortOne 취소 API는 호출되지 않는다
        verify(portOneClient, never()).cancelPayment(anyString(), any());
    }

    @Test
    @DisplayName("이미 환불 완료된 결제에 재환불을 시도하면 DUPLICATE_REFUND_REQUEST 예외가 발생한다")
    void refundPayment_중복환불요청_DUPLICATE_REFUND_REQUEST() {
        // given: 이미 COMPLETED 상태의 환불 이력이 존재
        User user = createUser(1L, 0, 0);
        Order order = createOrder(1L, user, OrderStatus.REFUNDED, 100000, 0);
        Payment payment = createPayment(1L, order, PaidStatus.REFUNDED, 100000);

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(true);

        // when & then: 중복 환불 방어 로직에서 DUPLICATE_REFUND_REQUEST 예외 발생
        assertThatThrownBy(() -> refundService.refundPayment(1L, "PMN-TEST-1", createRefundRequest("변심")))
                .isInstanceOf(RefundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_REFUND_REQUEST);

        verify(portOneClient, never()).cancelPayment(anyString(), any());
    }

    @Test
    @DisplayName("타유저의 주문을 환불하려 하면 USER_FORBIDDEN 예외가 발생한다")
    void refundPayment_타유저주문_USER_FORBIDDEN() {
        // given: userId=1 유저의 주문인데 userId=2가 환불 시도
        User user = createUser(1L, 0, 0);
        Order order = createOrder(1L, user, OrderStatus.COMPLETED, 100000, 0);
        Payment payment = createPayment(1L, order, PaidStatus.SUCCESS, 100000);

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(false);

        // when & then: 소유권 검증에서 USER_FORBIDDEN 예외 발생
        assertThatThrownBy(() -> refundService.refundPayment(2L, "PMN-TEST-1", createRefundRequest("변심")))
                .isInstanceOf(RefundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_FORBIDDEN);
    }

    @Test
    @DisplayName("COMPLETED 상태가 아닌 주문을 환불하려 하면 ORDER_CANNOT_REFUND 예외가 발생한다")
    void refundPayment_PENDING상태주문_ORDER_CANNOT_REFUND() {
        // given: 아직 결제가 완료되지 않은 PENDING 상태 주문 환불 시도
        User user = createUser(1L, 0, 0);
        Order order = createOrder(1L, user, OrderStatus.PENDING, 100000, 0);
        Payment payment = createPayment(1L, order, PaidStatus.PENDING, 100000);

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(false);

        // when & then: 환불 가능 상태 검증에서 ORDER_CANNOT_REFUND 예외 발생
        assertThatThrownBy(() -> refundService.refundPayment(1L, "PMN-TEST-1", createRefundRequest("변심")))
                .isInstanceOf(RefundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CANNOT_REFUND);
    }

    @Test
    @DisplayName("존재하지 않는 portOneId로 환불을 시도하면 PAYMENT_NOT_FOUND 예외가 발생한다")
    void refundPayment_존재하지않는portOneId_PAYMENT_NOT_FOUND() {
        // given: DB에 없는 portOneId
        given(paymentRepository.findByPortOneId("PMN-GHOST")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> refundService.refundPayment(1L, "PMN-GHOST", createRefundRequest("변심")))
                .isInstanceOf(RefundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("환불 처리 중 예외 발생 시 환불 실패 이력이 저장된다")
    void refundPayment_처리중예외발생_실패이력저장() {
        // given: PortOne 취소 API 호출 중 런타임 예외 발생
        User user = createUser(1L, 0, 100000);
        Order order = createOrder(1L, user, OrderStatus.COMPLETED, 100000, 0);
        Payment payment = createPayment(1L, order, PaidStatus.SUCCESS, 100000);

        PortOneProperties.Store store = new PortOneProperties.Store();
        ReflectionTestUtils.setField(store, "id", "STORE_001");

        given(paymentRepository.findByPortOneId("PMN-TEST-1")).willReturn(Optional.of(payment));
        given(refundRepository.existsByPaymentIdAndRefundStatus(1L, RefundStatus.COMPLETED)).willReturn(false);
        given(portOneProperties.getStore()).willReturn(store);
        given(portOneClient.cancelPayment(anyString(), any(PortOneCancelPaymentRequest.class)))
                .willThrow(new RuntimeException("PortOne 서버 오류"));

        // when & then: 예외가 외부로 전파되고 실패 이력이 저장된다
        assertThatThrownBy(() -> refundService.refundPayment(1L, "PMN-TEST-1", createRefundRequest("변심")))
                .isInstanceOf(RuntimeException.class);

        verify(refundRecordService).saveFailRefund(any(Payment.class), anyString());
    }
}