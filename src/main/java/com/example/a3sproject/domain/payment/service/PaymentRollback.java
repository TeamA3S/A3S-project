package com.example.a3sproject.domain.payment.service;

import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 롤백용 클래스
@Component
@RequiredArgsConstructor
public class PaymentRollback {
    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failPayment(Payment payment) {
        payment.failStatus();
        paymentRepository.save(payment);
        // 트랜잭션 A가 롤백돼도 이건 이미 커밋됨! ✅
    }
}