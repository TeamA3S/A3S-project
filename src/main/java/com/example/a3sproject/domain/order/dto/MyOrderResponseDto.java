package com.example.a3sproject.domain.order.dto;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyOrderResponseDto {
    private final String orderNumber;            // 주문번호
    private final int totalAmount;               // 총 금액
    private final OrderStatus orderStatus;       // 주문 상태
    private final LocalDateTime createdAt;       // 주문일

    public static MyOrderResponseDto from(Order order) {
        return MyOrderResponseDto.builder()
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
