package com.example.a3sproject.domain.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
public class CreateOrderRequestDto {

    @JsonProperty("items")
    @NotEmpty(message = "주문 상품은 1개 이상이어야 합니다.")
    @Valid
    private List<OrderItemRequestDto> orderItems;


    @Getter
    @NoArgsConstructor
    public static class OrderItemRequestDto {
        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 1개 이상이어야 합니다.")
        private Integer quantity;
    }
}
