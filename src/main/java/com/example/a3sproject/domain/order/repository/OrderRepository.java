package com.example.a3sproject.domain.order.repository;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 주문 상세조회시 본인 주문이 맞는지 검증을 위한 조회메서드
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "user"})
    Optional<Order> findByIdAndUser_Id(Long orderId, Long userId);

    // 특정 유저의 특정 상태 주문 조회 (상태별 필터링 기능 대비)
    List<Order> findByUserAndOrderStatus(User user, OrderStatus orderStatus);

}
