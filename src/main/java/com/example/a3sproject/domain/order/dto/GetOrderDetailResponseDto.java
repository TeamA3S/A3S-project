package com.example.a3sproject.domain.order.dto;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class GetOrderDetailResponseDto {

    private final Long orderId;            // 주문 ID
    private final String orderNumber;      // 주문번호
    private final int totalAmount;         // 주문 총액
    private final String currency;         // 통화
    private final OrderStatus orderStatus; // 주문 상태
    private final LocalDateTime createdAt; // 주문 생성 시간
    private final List<OrderItemDetailDto> orderItems;

    /*
    // ===== 결제/포인트 구현 후 활성화할 필드 =====
    // 포인트 사용 후 최종 결제 금액, 적립 포인트, 결제 정보는
    // 요구사항상 주문 상세 조회에 포함되어야 함
    // private final Integer usedPoints;      // 사용 포인트
    // private final Integer finalAmount;     // 포인트 반영 후 최종 결제 금액
    // private final Integer earnedPoints;    // 적립 포인트
    // private final String paymentStatus;    // 결제 상태
    // private final String paymentMethod;    // 결제 수단
    // private final Integer paidAmount;      // 실제 결제 금액
    */

    private GetOrderDetailResponseDto(
            Long orderId,
            String orderNumber,
            int totalAmount,
            String currency,
            OrderStatus orderStatus,
            LocalDateTime createdAt,
            List<OrderItemDetailDto> orderItems

            /*
            // ===== 결제/포인트 구현 후 constructor에 추가 =====
            , Integer usedPoints
            , Integer finalAmount
            , Integer earnedPoints
            , String paymentStatus
            , String paymentMethod
            , Integer paidAmount
            */
    ) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
        this.orderItems = orderItems;

        /*
        // ===== 결제/포인트 구현 후 assignment 활성화 =====
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.earnedPoints = earnedPoints;
        this.paymentStatus = paymentStatus;
        this.paymentMethod = paymentMethod;
        this.paidAmount = paidAmount;
        */
    }

    public static GetOrderDetailResponseDto of(Order order) {
        return new GetOrderDetailResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                "KRW",
                order.getOrderStatus(),
                order.getCreatedAt(),
                order.getOrderItems().stream()
                        .map(OrderItemDetailDto::new)
                        .toList()

                /*
                // ===== 결제/포인트 구현 후 인자 추가 =====
                , usedPoints
                , finalAmount
                , earnedPoints
                , paymentStatus
                , paymentMethod
                , paidAmount
                */
        );
    }

    /*
    // ===== 결제/포인트 구현 후 사용할 확장 팩토리 메서드 =====
    public static GetOrderDetailResponseDto of(
            Order order,
            Integer usedPoints,
            Integer earnedPoints,
            Payment payment
    ) {
        return new GetOrderDetailResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                "KRW",
                order.getOrderStatus(),
                order.getCreatedAt(),
                order.getOrderItems().stream()
                        .map(OrderItemDetailDto::new)
                        .toList(),
                usedPoints,
                order.getTotalAmount() - usedPoints, // finalAmount
                earnedPoints,
                payment != null ? payment.getPaymentStatus().name() : null,
                payment != null ? payment.getPaymentMethod().name() : null,
                payment != null ? payment.getAmount() : null
        );
    }
    */

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