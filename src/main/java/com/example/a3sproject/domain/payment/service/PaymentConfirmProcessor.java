package com.example.a3sproject.domain.payment.service;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.payment.dto.response.PaymentConfirmResponse;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.point.service.PointService;
import com.example.a3sproject.domain.portone.dto.response.PortOnePaymentResponse;
import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;



// PaymentConfirmProcessor - 트랜잭션 B (결제 확정)
@Component
@RequiredArgsConstructor
public class PaymentConfirmProcessor {
    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Transactional
    public PaymentConfirmResponse confirm(Payment payment, PortOnePaymentResponse portOneResponse) {
        // PortOne 결제 상태 검증
        if (PortOnePayStatus.PAID != portOneResponse.status()) {
            throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
        }

        // 금액 검증
        if (portOneResponse.amount().total() != payment.getPaidAmount()) {
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 재고 차감
        for (OrderItem orderItem : payment.getOrder().getOrderItems()) {
            orderItem.getProduct().decreaseStock(orderItem.getQuantity());
        }

        // 최종 확정
        payment.confirmPayment(portOneResponse.paidAt()); // 상태 변경
        payment.getOrder().updateOrderStatus(OrderStatus.COMPLETED);  // 주문 상태 성공으로 변경
        paymentRepository.save(payment);

        // 유저 총 결제금액 업데이트
        User user = userRepository.findWithLockById(payment.getOrder().getUser().getId()).orElseThrow(
                () -> new PaymentException(ErrorCode.USER_NOT_FOUND)
        );
        user.updateTotalPaymentAmount(payment.getPaidAmount());

        // 포인트 적립
        Membership membership = membershipRepository.findWithLockByUser(user)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));
        int earnedPoint = (int)(payment.getPaidAmount() * membership.getEarnRate());

        pointService.earnPoint(user.getId(), payment.getOrder().getId(), earnedPoint);
        // 멤버십 등급 갱신
        membership.updateGrade(user.getTotalPaymentAmount());

        return new PaymentConfirmResponse(payment.getOrder().getOrderNumber(), "결제가 완료되었습니다.");
    }
}