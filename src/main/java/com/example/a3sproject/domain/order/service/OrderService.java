package com.example.a3sproject.domain.order.service;

import com.example.a3sproject.domain.order.dto.CreateOrderRequestDto;
import com.example.a3sproject.domain.order.dto.CreateOrderResponseDto;
import com.example.a3sproject.domain.order.dto.GetOrderDetailResponseDto;
import com.example.a3sproject.domain.order.dto.GetOrderListResponseDto;
import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.repository.OrderRepository;
import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.OrderException;
import com.example.a3sproject.global.exception.domain.ProductException;

import com.example.a3sproject.global.exception.domain.UserException;
import com.example.a3sproject.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepository;

    /**
     * 예) [요청]
     * {
     *   "orderItems": [
     *     { "productId": 1, "quantity": 2 },  // 나이키 신발 2개
     *     { "productId": 1, "quantity": 1 },  // 나이키 신발 1개 (중복!)
     *     { "productId": 3, "quantity": 1 }   // 아디다스 양말 1개
     *   ]
     * }
     */

    // 주문 생성
    @Transactional
    public CreateOrderResponseDto createOrder(Long userId, CreateOrderRequestDto requestDto) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new OrderException(ErrorCode.USER_NOT_FOUND)
        );
        // 주문 상품이 없으면 예외
        if (requestDto.getOrderItems() == null || requestDto.getOrderItems().isEmpty()) {
            throw new OrderException(ErrorCode.INVALID_INPUT);
        }

        // 같은 상품이 여러 번 들어오면 수량 합치기
        // 각 상품의 id와 수량를 뽑는다. -> quantityByProductId
        // 나이키 상품의 id = 1, 수량 = 2
        // 나이키 상품의 id = 1, 수량 = 1
        // 아디다스 상품의 id = 3, 수량 = 1
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
        for (CreateOrderRequestDto.OrderItemRequestDto item : requestDto.getOrderItems()) {
            // 수량 미입력 or 음수 -> 예외
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderException(ErrorCode.INVALID_INPUT);
            }
            // 중복 상품 수량 합치기
            // { 나이키 : 3개 }
            // { 아디다스 : 1개 } ...
            quantityByProductId.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        // 한 번에 상품 조회
        // findAllById([1, 3]) 여기서 1, 3는 각 상품의 id임. 즉, 상품의 id별로 찾아서 한 번에 조회한다.
        List<Product> products = productRepository.findAllById(quantityByProductId.keySet());

        // 존재하는 상품들인지 확인
        // 요청 상품 2개 (상품id: 1, 3)
        // 조회된 상품 2개 (나이키, 아디다스) -> 2 : 2 일치
        // 일치하지 않다면 예외
        if (products.size() != quantityByProductId.size()) {
            throw new OrderException(ErrorCode.ORDERITEM_NOT_FOUND);
        }

        // 상품 리스트를 맵으로 변환 (id를 키로, 상품을 값으로)
        // product(id=1, "나이키 신발", 100000)
        // product(id=3, "아디다스 신발", 50000)
        // ->
        // {
        //  1 : 나이키 신발,
        //  3 : 아디다스 신발
        // }
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 주문 상품을 담을 리스트
        List<OrderItem> orderItems = new ArrayList<>();

        // entrySet() = (id, quantity) -> entry = 상품id와 수량 한 묶음
        for (Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {
            // 상품id로 상품객체를 찾는다
            Product product = productMap.get(entry.getKey());
            // 수량 가져오기
            Integer quantity = entry.getValue();

            // 해당 상품의 수량체크 (재고 사전확인)
            validateProductForOrder(product, quantity);
            // 상품과 수량을 주문상품에 생성 -> 주문상품 리스트에 추가
            orderItems.add(OrderItem.createOrderItem(product, quantity));
        }

        // 주문번호 발급
        String orderNumber = generateOrderNumber();

        // 주문 상품 리스트, 주문번호를 기반으로 주문 생성
        Order order = Order.createOrder(user, orderItems, orderNumber);

        // DB에 저장
        Order savedOrder = orderRepository.save(order);

        // dto에 담아서 응답 반환
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

    // 주문 목록 조회
    public List<GetOrderListResponseDto> getAllOrderList(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(order -> GetOrderListResponseDto.of(
                        order,
                        null,
                        order.getTotalAmount(),
                        null
                )).toList();
    }

    // 주문 상세 조회
    public GetOrderDetailResponseDto getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(
                () -> new OrderException(ErrorCode.ORDER_NOT_FOUND)
        );
        return GetOrderDetailResponseDto.of(order);
    }


}
