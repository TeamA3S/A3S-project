package com.example.a3sproject.domain.refund.service;

import com.example.a3sproject.config.PortOneProperties;
import com.example.a3sproject.domain.membership.entity.Membership;
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
import com.example.a3sproject.domain.portone.dto.request.PortOneCancelPaymentRequest;
import com.example.a3sproject.domain.portone.dto.response.PortOneCancelPaymentResponse;
import com.example.a3sproject.domain.refund.dto.request.RefundRequestDto;
import com.example.a3sproject.domain.refund.dto.response.RefundResponseDto;
import com.example.a3sproject.domain.refund.enums.RefundStatus;
import com.example.a3sproject.domain.refund.repository.RefundRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.RefundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final RefundRecordService refundRecordService;
    private final PointService pointService;
    private final PointRepository pointRepository;
    private final PortOneProperties portOneProperties;
    private final MembershipRepository membershipRepository;

    @Transactional
    public RefundResponseDto refundPayment(Long userId, String portOneId, RefundRequestDto requestDto) {
        // portOneId로 Payment 조회
        Payment payment = paymentRepository.findByPortOneId(portOneId).orElseThrow(
                () -> new RefundException(ErrorCode.PAYMENT_NOT_FOUND)
        );

        // 이미 진행된 환불인지 확인함
        boolean alreadyRefunded = refundRepository.existsByPaymentIdAndRefundStatus(
                payment.getId(), RefundStatus.COMPLETED);
        if (alreadyRefunded) {
            throw new RefundException(ErrorCode.DUPLICATE_REFUND_REQUEST);
        }

        // 본인 주문인지 확인
        Order order = payment.getOrder();
        if (!order.getUser().getId().equals(userId)) {
            throw new RefundException(ErrorCode.USER_FORBIDDEN);
        }
        // 환불 가능한 상태인지 확인: 주문상태(COMPLETED), 결제상태(SUCCESS)만 가능
        if (!order.getOrderStatus().equals(OrderStatus.COMPLETED) || !payment.getPaidStatus().equals(PaidStatus.SUCCESS)) {
            throw new RefundException(ErrorCode.ORDER_CANNOT_REFUND);
        }
        // [수정된 RefundService.java 의 try 블록 내부]
        try {
            // 💡 1. 취소 시각을 담을 변수를 미리 선언합니다. 기본값은 우리 서버의 현재 시간!
            java.time.OffsetDateTime cancelledAt = java.time.OffsetDateTime.now();

            if (payment.getPaidAmount() > 0) {
                // 💡 2. 실제 돈 결제가 들어간 경우에만 포트원 API를 호출합니다.
                PortOneCancelPaymentResponse portOneCancelPaymentResponse = portOneClient.cancelPayment(
                        portOneId,
                        new PortOneCancelPaymentRequest(requestDto.getReason(), portOneProperties.getStore().getId())
                );
                // 💡 3. 포트원에서 정상적으로 환불되면, 포트원이 보내준 취소 시각으로 덮어씌웁니다.
                cancelledAt = portOneCancelPaymentResponse.cancellation().cancelledAt();
            }

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

            // 💡 4. 위에서 구한 안전한 cancelledAt 변수를 사용합니다! (NPE 해결)
            refundRecordService.saveSuccessRefund(payment, requestDto.getReason(), cancelledAt);

            // 💡 5. 결제상태 환불완료로 변경할 때도 안전한 변수 사용!
            payment.refundStatus(cancelledAt);

            // 주문상태 환불완료로 변경
            order.markRefunded();

            // 총 결제금액 차감 후 등급 재계산
            order.getUser().updateTotalPaymentAmount(-payment.getPaidAmount());

            Membership membership = membershipRepository.findWithLockByUser(order.getUser()).orElseThrow(
                    () -> new RefundException(ErrorCode.USER_FORBIDDEN)
            );
            membership.updateGrade(order.getUser().getTotalPaymentAmount());

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
