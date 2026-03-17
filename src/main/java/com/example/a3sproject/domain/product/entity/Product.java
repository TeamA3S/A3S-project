package com.example.a3sproject.domain.product.entity;


import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus productStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory productCategory;

    // TODO: 추후 주문 엔터티와 연관관계 매핑.
    // TODO: 추후 동시성 문제 -> 비관or낙관 락

    @Builder
    public Product(String name, int price, int stock, String description, ProductStatus productStatus, ProductCategory productCategory) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.productStatus = productStatus;
        this.productCategory = productCategory;
    }

    // 상품 상태 변경
    public void updateStatus(ProductStatus productStatus){
        this.productStatus = productStatus;
    }

    // 재고 차감
    public void decreaseStock(int quantity){
        if (this.stock - quantity < 0) {
            throw new IllegalArgumentException("재고가 부족합니다."); // TODO: 커스텀예외 필요
        }
        this.stock = this.stock - quantity;
    }
    // 재고 증가 (환불/주문취소 시 재고 복구)
    public void increaseStock(int quantity) {
        this.stock = this.stock + quantity;
    }

}
