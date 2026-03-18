package com.example.a3sproject.domain.payment.repository;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 멱등성 검증용
    boolean existsByPortOneIdAndPaidStatus(String portOneId, PaidStatus paidStatus);
    // 결제 확정시 탐색
    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByPaymentUuid(String paymentUuid);
}
