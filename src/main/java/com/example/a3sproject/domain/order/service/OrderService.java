package com.example.a3sproject.domain.order.service;

import com.example.a3sproject.domain.order.dto.CreateOrderRequestDto;
import com.example.a3sproject.domain.order.dto.CreateOrderResponseDto;
import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.repository.OrderRepository;
import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.OrderException;
import com.example.a3sproject.global.exception.domain.ProductException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    // 주문 생성
    @Transactional
    public CreateOrderResponseDto createOrder(User user, CreateOrderRequestDto requestDto) {
        if (requestDto.getOrderItems() == null || requestDto.getOrderItems().isEmpty()) {
            throw new OrderException(ErrorCode.INVALID_INPUT);
        }

        // 같은 상품이 여러 번 들어오면 수량 합치기
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
        for (CreateOrderRequestDto.OrderItemRequestDto item : requestDto.getOrderItems()) {
            // 중복 상품 수량 합치기
            quantityByProductId.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        // 한 번에 상품 조회
        List<Product> products = productRepository.findAllById(quantityByProductId.keySet());

        // 존재하는 상품들인지 확인
        if (products.size() != quantityByProductId.size()) {
            throw new OrderException(ErrorCode.ORDERITEM_NOT_FOUND);
        }

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {
            Product product = productMap.get(entry.getKey());
            Integer quantity = entry.getValue();

            validateProductForOrder(product, quantity);

            orderItems.add(OrderItem.createOrderItem(product, quantity));
        }

        String orderNumber = generateOrderNumber();

        Order order = Order.createOrder(user, orderItems, orderNumber);
        Order savedOrder = orderRepository.save(order);

        return new CreateOrderResponseDto(
                savedOrder.getId(),
                savedOrder.getTotalAmount(),
                savedOrder.getOrderNumber()
        );
    }

    // 상품 상태, 재고 사전 확인 (주문할 수 있는 상품인지)
    private void validateProductForOrder(Product product, int quantity) {
         if (product.getProductStatus() != ProductStatus.ON_SALE) {
             throw new OrderException(ErrorCode.ORDERITEM_UNAVAILABLE);
         }
        // 재고 사전 확인
        if (product.getStock() < quantity) {
            throw new ProductException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }
    }

    // 주문번호 발급 (UUID)
    private String generateOrderNumber() {
        return "ODN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

}
