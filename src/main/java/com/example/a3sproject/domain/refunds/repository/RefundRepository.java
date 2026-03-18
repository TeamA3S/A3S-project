package com.example.a3sproject.domain.refunds.repository;

import com.example.a3sproject.domain.refunds.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
