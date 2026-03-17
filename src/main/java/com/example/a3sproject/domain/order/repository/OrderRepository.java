package com.example.a3sproject.domain.order.repository;

import com.example.a3sproject.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
