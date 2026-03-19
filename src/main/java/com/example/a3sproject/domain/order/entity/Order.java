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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    private int usedPointAmount;   // 사용 포인트
    private int finalAmount;       // 최종 결제 금액


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

    // 결제 확정 후 결제완료로 변경
    public void markPaid() {
        // 대기상태가 아니면 에러
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new OrderException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.orderStatus = OrderStatus.COMPLETED;
    }

    // 환불 완료 후 환불로 변경
    public void markRefunded() {
        if (this.orderStatus != OrderStatus.COMPLETED) {
            throw new OrderException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.orderStatus = OrderStatus.REFUNDED;
    }

    // 포인트 적용
    public void applyPointUsage(int usedPointAmount) {
        if (usedPointAmount < 0 || usedPointAmount > this.totalAmount) {
            throw new OrderException(ErrorCode.INVALID_INPUT);
        }
        this.usedPointAmount = usedPointAmount;
        this.finalAmount = this.totalAmount - usedPointAmount;
    }
}


