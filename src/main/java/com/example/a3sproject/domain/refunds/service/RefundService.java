package com.example.a3sproject.domain.refunds.service;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.payment.service.PaymentService;
import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import com.example.a3sproject.domain.point.service.PointService;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentRequest;
import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentResponse;
import com.example.a3sproject.domain.refunds.dto.request.RefundRequestDto;
import com.example.a3sproject.domain.refunds.dto.response.RefundResponseDto;
import com.example.a3sproject.domain.refunds.entity.Refund;
import com.example.a3sproject.domain.refunds.repository.RefundRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import com.example.a3sproject.global.exception.domain.RefundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final RefundRecordService refundRecordService;
    private final PointService pointService;
    private final PointRepository pointRepository;

    @Transactional
    public RefundResponseDto refundPayment(Long userId, String portOneId, RefundRequestDto requestDto) {
        // portOneId로 Payment 조회
        Payment payment = paymentRepository.findByportOneId(portOneId).orElseThrow(
                () -> new RefundException(ErrorCode.PAYMENT_NOT_FOUND)
        );
        // 본인 주문인지 확인
        Order order = payment.getOrder();
        if (!order.getUser().getId().equals(userId)) {
            throw new RefundException(ErrorCode.USER_FORBIDDEN);
        }
        // 환불 가능한 상태인지 확인: 주문상태(COMPLETED), 결제상태(SUCCESS)만 가능
        if (!order.getOrderStatus().equals(OrderStatus.COMPLETED) || !payment.getPaidStatus().equals(PaidStatus.SUCCESS)) {
            throw new RefundException(ErrorCode.ORDER_CANNOT_REFUND);
        }
        try {
            // portOne 환분 API 호출
            PortOneCancelPaymentResponse portOneCancelPaymentResponse =
                    portOneClient.cancelPayment(portOneId, new PortOneCancelPaymentRequest(requestDto.getReason()));

            // 사용한 포인트 복구
            if (order.getUsedPointAmount() > 0) {
                pointService.restorePoint(
                        userId,
                        order.getId(),
                        order.getUsedPointAmount()
                );
            }
            // 적립된 포인트 취소
            List<PointTransaction> earnedTransactions =
                    pointRepository.findByOrderIdAndType(order.getId(), PointTransactionType.EARN);

            int totalEarned = earnedTransactions.stream()
                    .mapToInt(PointTransaction::getPoints)
                    .sum();

            if (totalEarned > 0) {
                pointService.cancelEarnedPoint(
                        userId,
                        order.getId(),
                        totalEarned
                );
            }
            // 성공 이력 저장
            refundRecordService.saveSuccessRefund(payment, requestDto.getReason(), portOneCancelPaymentResponse.cancelledAt());

            // 결제상태 환불완료로 변경
            payment.refundStatus(portOneCancelPaymentResponse.cancelledAt());
            // 주문상태 환불완료로 변경
            order.markRefunded();

            // 재고 복구
            for (OrderItem orderItem : order.getOrderItems()) {
                orderItem.getProduct().increaseStock(orderItem.getQuantity());
            }
            return RefundResponseDto.of(order);
        } catch (Exception e) {
            // 실패시 실패 이력 저장
            refundRecordService.saveFailRefund(payment, requestDto.getReason());
            throw e;
        }
    }
}
