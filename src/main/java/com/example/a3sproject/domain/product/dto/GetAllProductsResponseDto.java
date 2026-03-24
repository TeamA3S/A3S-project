package com.example.a3sproject.domain.product.dto;

import lombok.Getter;

@Getter
public class GetAllProductsResponseDto {

    private final Long productId;
    private final String name;
    private final int price;
    private final int stock;

    public GetAllProductsResponseDto(Long productId, String name, int price, int stock) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}
