package com.example.a3sproject.domain.payments.refund.repository;

import com.example.a3sproject.domain.payments.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
