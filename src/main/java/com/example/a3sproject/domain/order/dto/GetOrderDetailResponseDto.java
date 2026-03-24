package com.example.a3sproject.domain.order.dto;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class GetOrderDetailResponseDto {

    private final Long orderId;            // 주문 ID
    private final String orderNumber;      // 주문번호
    private final int totalAmount;         // 주문 원금액
    private final int usedPoints;          // 사용 포인트 스냅샷
    private final int finalAmount;         // 포인트 반영 후 최종 결제 금액
    private final int earnedPoints;        // 현재 유효한 적립 포인트
    private final String currency;         // 통화
    private final OrderStatus orderStatus; // 주문 상태
    private final PaidStatus paymentStatus; // 결제 상태
    private final Integer paidAmount;      // 실제 결제 금액 (결제 시도/완료 없으면 null)
    private final LocalDateTime createdAt; // 주문 생성 시간
    private final List<OrderItemDetailDto> orderItems;

    private GetOrderDetailResponseDto(
            Long orderId,
            String orderNumber,
            int totalAmount,
            int usedPoints,
            int finalAmount,
            int earnedPoints,
            String currency,
            OrderStatus orderStatus,
            PaidStatus paymentStatus,
            Integer paidAmount,
            LocalDateTime createdAt,
            List<OrderItemDetailDto> orderItems
    ) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.earnedPoints = earnedPoints;
        this.currency = currency;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
        this.paidAmount = paidAmount;
        this.createdAt = createdAt;
        this.orderItems = orderItems;
    }

    public static GetOrderDetailResponseDto of(Order order, int earnedPoints, Payment payment) {
        return new GetOrderDetailResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getUsedPointAmount(),
                order.getFinalAmount(),
                earnedPoints,
                "KRW",
                order.getOrderStatus(),
                payment != null ? payment.getPaidStatus() : null,
                payment != null ? payment.getPaidAmount() : null,
                order.getCreatedAt(),
                order.getOrderItems().stream()
                        .map(OrderItemDetailDto::new)
                        .toList()
        );
    }

    @Getter
    public static class OrderItemDetailDto {
        private final Long orderItemId;
        private final Long productId;
        private final String productName;
        private final int unitPrice;
        private final int quantity;
        private final int lineAmount;

        public OrderItemDetailDto(OrderItem orderItem) {
            this.orderItemId = orderItem.getId();
            this.productId = orderItem.getProduct().getId();
            this.productName = orderItem.getProductName();
            this.unitPrice = orderItem.getUnitPrice();
            this.quantity = orderItem.getQuantity();
            this.lineAmount = orderItem.getUnitPrice() * orderItem.getQuantity();
        }
    }
}