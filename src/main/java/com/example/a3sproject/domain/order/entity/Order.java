package com.example.a3sproject.domain.order.entity;

import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.entity.BaseEntity;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.OrderException;
import com.example.a3sproject.global.security.CustomUserDetails;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

@Getter
@Table(name = "orders")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    // TODO: 추후 동시성 문제 -> 비관or낙관 락

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    // 주문일은 생성될때 BaseEntity 활용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();


    private Order(User user, String orderNumber) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.orderStatus = OrderStatus.PENDING;
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    private int calculateTotalAmount() {
        return this.orderItems.stream()
                .mapToInt(OrderItem::getLineAmount)
                .sum();
    }

    // 주문생성 정적 메서드
    public static Order createOrder(User user, List<OrderItem> orderItems, String orderNumber) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new OrderException(ErrorCode.INVALID_INPUT);
        }

        Order order = new Order(user, orderNumber);
        orderItems.forEach(order::addOrderItem);
        order.totalAmount = order.calculateTotalAmount();
        return order;
    }

    // 주문 상태 변경 메서드
    public void updateOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
}


