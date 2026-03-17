package com.example.a3sproject.domain.order.dto;

import lombok.Getter;

@Getter
public class CreateOrderResponseDto {

    private final Long orderId;
    private final Integer totalAmount;
    private final String orderNumber;

    public CreateOrderResponseDto(Long orderId, Integer totalAmount, String orderNumber) {
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.orderNumber = orderNumber;
    }
}
