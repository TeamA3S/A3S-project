package com.example.a3sproject.domain.payment.service;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.order.repository.OrderRepository;
import com.example.a3sproject.domain.order.service.OrderService;
import com.example.a3sproject.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.a3sproject.domain.payment.dto.request.PaymentTryRequest;
import com.example.a3sproject.domain.payment.dto.response.PaymentConfirmResponse;
import com.example.a3sproject.domain.payment.dto.response.PaymentTryResponse;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.portone.dto.PortOnePaymentResponse;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import com.example.a3sproject.global.common.GenerateCodeUuid;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PortOneClient portOneClient;
    private final OrderService orderService;

    @Transactional // 결제 시도(생성) 메서드
    public PaymentTryResponse createPayment(Long userId, PaymentTryRequest request) {
        // 1. Order 조회
        Order order = orderRepository.findByIdAndUser_Id(request.orderId(), userId).orElseThrow(
                ()-> new PaymentException(ErrorCode.ORDER_NOT_FOUND)
        );
        // 결제 가능한 주문 상태인지 확인
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new PaymentException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }
        // 3. 사용 포인트 정규화
        int pointsToUse = request.pointsToUse() == null ? 0 : request.pointsToUse();

        if (pointsToUse < 0) {
            throw new PaymentException(ErrorCode.INVALID_INPUT);
        }

        // 주문 총액보다 많이 쓰면 안 됨
        if (pointsToUse > order.getTotalAmount()) {
            throw new PaymentException(ErrorCode.INVALID_INPUT);
        }

        // 4. 현재 포인트 잔액 검증
        // User에 pointBalance가 있다는 전제
        int currentPointBalance = order.getUser().getPointBalance();

        if (pointsToUse > currentPointBalance) {
            throw new PaymentException(ErrorCode.POINT_NOT_ENOUGH);
        }
        // 5. 최종 결제 금액 계산
        int finalPaidAmount = order.getTotalAmount() - pointsToUse;

        // 클라이언트가 보낸 금액은 신뢰하지 않고 서버 계산값과 비교
        if (request.totalAmount() != finalPaidAmount) {
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        // 6. 주문 스냅샷 갱신
        // 일반 결제면 usedPointAmount=0, finalAmount=totalAmount
        // 복합 결제면 usedPointAmount>0, finalAmount=totalAmount-usedPointAmount
        order.applyPointUsage(pointsToUse);

        Payment payment = paymentRepository.findByOrder(order) // 해당 결제와 관련된 주문이 이미 있는 지 확인
                .map(existing -> {  // 있다면 map 순회
                    if (existing.isFinalized()) {  // 이미 끝난 결제라면 에러
                        throw new PaymentException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
                    }
                    // 아니라면 다시 결제 시도 가능한 상태로 덮어쓰기
                    existing.preparePendingAttempt(finalPaidAmount);
                    return existing;
                })
                // 없다면 새로운 결제 만들기
                .orElseGet(() -> new Payment(
                        order,
                        finalPaidAmount,
                        GenerateCodeUuid.generateCodeUuid("PMN")
                ));
        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentTryResponse(
                savedPayment.getPaymentUuid(),
                orderService.buildOrderName(order),
                savedPayment.getPaidAmount(),
                "KRW"
        );
    }

    // Client Confirm 전용 - orderId로 조회
    @Transactional
    public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        Order order = orderRepository.findByIdAndUser_Id(request.orderId(), userId).orElseThrow(
                ()-> new PaymentException(ErrorCode.ORDER_NOT_FOUND)
        );
        Payment payment = paymentRepository.findByOrder(order).orElseThrow(
                ()-> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND)
        );

        // attempt 때 서버가 발급한 paymentId와 confirm 값이 같아야 함
        if (!payment.getPaymentUuid().equals(request.portOneId())) {
            throw new PaymentException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        processPaymentConfirm(payment, order, request.portOneId()); // 핵심 로직 호출
        return new PaymentConfirmResponse(order.getOrderNumber(), "결제가 완료되었습니다.");
    }

    // Webhook 전용 - paymentUuid로 조회
    @Transactional
    public void confirmPaymentByWebhook(String paymentUuid) {
        Payment payment = paymentRepository.findByPaymentUuid(paymentUuid).orElseThrow(
                ()-> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND)
        );
        Order order = payment.getOrder();
        processPaymentConfirm(payment, order, paymentUuid); // 동일한 핵심 로직 호출
    }

    // 결제 확정 메서드 - 공유 핵심 로직
    private void processPaymentConfirm(Payment payment, Order order, String portOneId) {
        // 2. 중복 요청 검증
        if(paymentRepository.existsByPortOneIdAndPaidStatus(portOneId, PaidStatus.SUCCESS)){
            throw new PaymentException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }
        // 3. PortOne 조회 API 호출
        PortOnePaymentResponse portOnePaymentResponse = portOneClient.getPayment(portOneId);
        // 3-1. PortOne 결제 상태 검증
        if(PortOnePayStatus.PAID != portOnePaymentResponse.status()){
            throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
        }
        // 4. 금액 검증
        if(portOnePaymentResponse.amount().total() != payment.getPaidAmount()){
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        } // 클라이언트가 보내온 request.payAmount()는 신뢰할 수 없는 값
        // 5. 재고 차감
        for (OrderItem orderItem : order.getOrderItems()) {
            orderItem.getProduct().decreaseStock(orderItem.getQuantity());
        }
        // 6. 최종 확정
        payment.confirmPayment(portOneId, portOnePaymentResponse.paidAt()); // 상태 변경
        order.updateOrderStatus(OrderStatus.COMPLETED); // 주문 상태 성공으로 변경
        paymentRepository.save(payment);

    }// Todo: 보상 트랜잭션 추가 해야함
}