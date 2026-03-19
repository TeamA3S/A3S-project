package com.example.a3sproject.domain.refunds.service;

import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.refunds.entity.Refund;
import com.example.a3sproject.domain.refunds.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class RefundRecordService {

    private final RefundRepository refundRepository;

    // 환불 성공 기록 저장
    @Transactional
    public void saveSuccessRefund(Payment payment, String reason, OffsetDateTime refundedAt) {
        Refund refund = new Refund(payment, reason);
        refund.completeRefund(refundedAt);
        refundRepository.save(refund);
    }
    // 환불 실패 기록 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailRefund(Payment payment, String reason) {
        Refund refund = new Refund(payment, reason);
        refund.cancelRefund();
        refundRepository.save(refund);
    }

}
