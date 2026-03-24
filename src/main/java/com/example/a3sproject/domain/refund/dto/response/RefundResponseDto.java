package com.example.a3sproject.domain.refund.dto.response;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import lombok.Getter;

@Getter
public class RefundResponseDto {

    private final boolean success;
    private final String paidStatus;
    private final String orderNumber;

    public RefundResponseDto(boolean success, String paidStatus, String orderNumber) {
        this.success = success;
        this.paidStatus = paidStatus;
        this.orderNumber = orderNumber;
    }

    public static RefundResponseDto of(Order order) {
        return new RefundResponseDto(
                true,
                PaidStatus.REFUNDED.name(),
                order.getOrderNumber()
                );
    }
}
