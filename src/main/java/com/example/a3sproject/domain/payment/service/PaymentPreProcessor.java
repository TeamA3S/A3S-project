package com.example.a3sproject.domain.payment.service;

import com.example.a3sproject.domain.payment.dto.PaymentPrepareResult;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.point.service.PointService;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// PaymentPreProcessor - 트랜잭션 A (사전 검증)
@Component
@RequiredArgsConstructor
public class PaymentPreProcessor {
    private final PaymentRepository paymentRepository;
    private final PointService pointService;

    @Transactional
    public PaymentPrepareResult validateAndPrepare(String portOneId, Long userId) {
        boolean pointUsed = false;
        Payment payment = paymentRepository.findByPortOneId(portOneId).orElseThrow(
                () -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND)
        );

        // 소유권 검증
        if (userId != null && !payment.getOrder().getUser().getId().equals(userId)) {
            throw new PaymentException(ErrorCode.USER_FORBIDDEN);
        }

        // 중복 요청 검증
        if (paymentRepository.existsByPortOneIdAndPaidStatus(portOneId, PaidStatus.SUCCESS)) {
            throw new PaymentException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }

        // 결제 금액 확인
        if(payment.getPaidAmount() == 0){
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_NOT_ENOUGH);
        }

        // 포인트 차감
        if (payment.getPointsToUse() > 0) {
            pointService.validateAndUse(userId, payment.getOrder().getId(), payment.getPointsToUse());
            pointUsed = true;
        }

        return new PaymentPrepareResult(payment, pointUsed, userId);
    }
}