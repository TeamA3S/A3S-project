package com.example.a3sproject.domain.order.entity;

import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.global.entity.BaseEntity;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.OrderException;
import com.example.a3sproject.global.exception.domain.ProductException;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Table(name = "order_items")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    void assignOrder(Order order) {
        this.order = order;
    }

    int getLineAmount() {
        return this.unitPrice * this.quantity;
    }

    private OrderItem(Product product, int quantity) {
        this.product = product;
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    public static OrderItem createOrderItem(Product product, int quantity) {
        if (product == null) {
            throw new OrderException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (quantity <= 0) {
            throw new OrderException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }
        return new OrderItem(product, quantity);
    }

}
