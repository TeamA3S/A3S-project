package com.example.a3sproject.domain.payment.service;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.order.repository.OrderRepository;
import com.example.a3sproject.domain.payment.dto.PaymentProcessResult;
import com.example.a3sproject.domain.payment.dto.request.PaymentTryRequest;
import com.example.a3sproject.domain.payment.dto.response.PaymentConfirmResponse;
import com.example.a3sproject.domain.payment.dto.response.PaymentTryResponse;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.point.service.PointService;
import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.portone.dto.PortOnePaymentResponse;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
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
    private final PortOneClient portOneClient;
    private final PointService pointService;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PaymentFailureHandler paymentFailureHandler;

    // 주문 이름 생성 메서드
    private String buildOrderName(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return "주문";
        }

        String firstItemName = order.getOrderItems().get(0).getProductName();
        int itemCount = order.getOrderItems().size();

        if (itemCount == 1) {
            return firstItemName;
        }
        return firstItemName + " 외 " + (itemCount - 1) + "건";
    }

    @Transactional // 결제 시도(생성) 메서드
    public PaymentTryResponse createPayment(long userId, PaymentTryRequest request) {
        int pointsForUse = request.pointsToUseOrZero();
        // 1. Order 조회
        Order order = orderRepository.findByIdAndUser_Id(request.orderId(), userId).orElseThrow(
                ()-> new PaymentException(ErrorCode.ORDER_NOT_FOUND)
        );
        // 결제 가능한 주문 상태인지 확인
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new PaymentException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }
        // 지금 단계는 일반 카드 결제만 구현
        if (pointsForUse < 0) {
            throw new PaymentException(ErrorCode.INVALID_INPUT);
        }
        int payableAmount = order.getTotalAmount();

        // 서버 계산 금액과 요청 금액 일치 검증
        if (request.totalAmount() == null || request.totalAmount() != payableAmount) {
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        int actualPayAmount = payableAmount - pointsForUse; // 클라이언트가 받아서 포트원에 요청할 실 결제 금액

        Payment payment = paymentRepository.findByOrder(order) // 해당 결제와 관련된 주문이 이미 있는 지 확인
                .map(existing -> {  // 있다면 map 순회
                    if (existing.isFinalized()) {  // 이미 끝난 결제라면 에러
                        throw new PaymentException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
                    }
                    // 아니라면 다시 결제 시도 가능한 상태로 덮어쓰기
                    existing.preparePendingAttempt(payableAmount);
                    return existing;
                })
                // 없다면 새로운 결제 만들기
                .orElseGet(() -> new Payment(
                        order,
                        actualPayAmount,
                        GenerateCodeUuid.generateCodeUuid("PMN"),
                        pointsForUse
                ));
        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentTryResponse(
                true,
                savedPayment.getPortOneId(),
                buildOrderName(order),
                actualPayAmount,
                "KRW",
                String.valueOf(savedPayment.getPaidStatus())
        );
    }

    // Confirm  - portOneId로 조회
    @Transactional
    public PaymentConfirmResponse confirmPayment(String portOneId, Long userId) {
        PaymentProcessResult result = null;
        Payment payment = null;
        try {
            payment = paymentRepository.findByportOneId(portOneId).orElseThrow(
                    () -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND)
            );
            if (userId != null) { // 소유권 검증, 웹훅은 검증 불필요
                if (!payment.getOrder().getUser().getId().equals(userId)) {
                    throw new PaymentException(ErrorCode.USER_FORBIDDEN);
                }
            } else {
                userId = payment.getOrder().getUser().getId();
            }

            result = processPaymentConfirm(payment, payment.getOrder(), portOneId, payment.getPointsToUse(), userId); // 핵심 로직 호출
            return new PaymentConfirmResponse(payment.getOrder().getOrderNumber(), "결제가 완료되었습니다.");
        } catch (Exception e) {
            // 보상 트랜잭션!
            paymentFailureHandler.handlePaymentFailure(payment, result, userId);
            throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
        }
    }

    // 결제 확정 메서드 - 공유 핵심 로직
    private PaymentProcessResult processPaymentConfirm(Payment payment, Order order, String portOneId, int pointsToUse, long userId) {
        boolean portOneConfirmed = false;

        // 1. 포인트 차감
        if (pointsToUse > 0) {
            pointService.validateAndUse(userId, pointsToUse);
        }
        // 2. 중복 요청 검증
        if (paymentRepository.existsByPortOneIdAndPaidStatus(portOneId, PaidStatus.SUCCESS)) {
            throw new PaymentException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }
        // 3. PortOne 조회 API 호출
        PortOnePaymentResponse portOnePaymentResponse = portOneClient.getPayment(portOneId);
        // 4. PortOne 결제 상태 검증
        if (PortOnePayStatus.PAID != portOnePaymentResponse.status()) {
            throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
        }
        // 5. 금액 검증
        if (portOnePaymentResponse.amount().total() != payment.getPaidAmount()) {
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        } // 클라이언트가 보내온 request.payAmount()는 신뢰할 수 없는 값
        // 6. 재고 차감
        for (OrderItem orderItem : order.getOrderItems()) {
            orderItem.getProduct().decreaseStock(orderItem.getQuantity());
        }
        portOneConfirmed = true;
        // 7. 최종 확정
        payment.confirmPayment(portOnePaymentResponse.paidAt()); // 상태 변경
        order.updateOrderStatus(OrderStatus.COMPLETED); // 주문 상태 성공으로 변경// PAID에서 COMPLETED로 수정
        paymentRepository.save(payment);
        // 8. 유저 총 결제금액 업데이트
        User user = userRepository.findWithLockById(order.getUser().getId()).orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));
        user.updateTotalPaymentAmount(payment.getPaidAmount());
        // 9. 멤버십 등급 갱신
        Membership membership = membershipRepository.findWithLockByUser(user)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));
        membership.updateGrade(user.getTotalPaymentAmount());
        // 10. 포인트 적립
        int earnedPoint = (int)(payment.getPaidAmount() * membership.getEarnRate());
        pointService.earnPoint(user.getId(), order.getId(), earnedPoint);
        // 11. 롤백을 위해 변경 여부 값 반환
        return new PaymentProcessResult(portOneConfirmed, portOneId);
    }
}