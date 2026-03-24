package com.example.a3sproject.domain.paymentMethod.repository;

import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
}
