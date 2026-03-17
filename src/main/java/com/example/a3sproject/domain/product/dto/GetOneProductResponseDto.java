package com.example.a3sproject.domain.product.dto;

import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductCategory;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import lombok.Getter;

@Getter
public class GetOneProductResponseDto {

    private final Long productId;
    private final String name;
    private final int price;
    private final int stock;
    private final String description;
    private final ProductStatus status;
    private final ProductCategory category;

    public GetOneProductResponseDto(Product product) {
        this.productId = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.stock = product.getStock();
        this.description = product.getDescription();
        this.status = product.getProductStatus();
        this.category = product.getProductCategory();
    }
}
