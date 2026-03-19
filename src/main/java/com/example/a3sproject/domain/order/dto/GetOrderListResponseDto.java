package com.example.a3sproject.domain.order.dto;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class GetOrderListResponseDto {

    private final Long orderId;          // 주문 Id
    private final String orderNumber;    // 주문 번호
    private final int totalAmount;       // 주문 금액

    private final Integer usedPoints;    // 사용한 포인트
    private final Integer finalAmount;   // 최종 결제 금액
    private final Integer earnedPoints;  // 적립된 포인트

    private final String currency;           // 통화
    @JsonProperty("status")
    private final String orderStatus;   // 주문 상태
    private final LocalDateTime createdAt;   // 주문 생성 시간

    // TODO: 결제 구현 후 적용
//    private final String paymentStatus;    // 결제 상태 (추후 Payment 구현 후 반영)
//    private final String paymentMethod;    // 결제 수단 (추후 반영)
//    private final Integer paidAmount;      // 실제 결제 금액 (추후 반영)

    public GetOrderListResponseDto(
            Long orderId,
            String orderNumber,
            int totalAmount,
            Integer usedPoints,
            Integer finalAmount,
            Integer earnedPoints,
            String currency,
            OrderStatus orderStatus,
            LocalDateTime createdAt
    ) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.earnedPoints = earnedPoints;
        this.currency = currency;
        this.orderStatus = orderStatus.name();
        this.createdAt = createdAt;
    }

    // TODO: 결제 파트 구현 후 실제 finalAmount로 교체필요!
    public static GetOrderListResponseDto of(
            Order order,
            Integer usedPoints,
            Integer finalAmount,
            Integer earnedPoints
    ) {
        return new GetOrderListResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                usedPoints,
                finalAmount,
                earnedPoints,
                "KRW",
                order.getOrderStatus(),
                order.getCreatedAt()
        );
    }

}
