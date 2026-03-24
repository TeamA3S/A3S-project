package com.example.a3sproject.domain.refund.repository;

import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.refund.entity.Refund;
import com.example.a3sproject.domain.refund.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    boolean existsByPaymentIdAndRefundStatus(Long paymentId, RefundStatus refundStatus);

    Optional<Refund> findByPayment(Payment payment);
}
