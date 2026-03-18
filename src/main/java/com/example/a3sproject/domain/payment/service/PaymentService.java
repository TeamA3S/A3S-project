package com.example.a3sproject.domain.payment.service;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.order.entity.OrderItem;
import com.example.a3sproject.domain.order.enums.OrderStatus;
import com.example.a3sproject.domain.order.repository.OrderRepository;
import com.example.a3sproject.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.a3sproject.domain.payment.dto.response.PaymentConfirmResponse;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.portone.dto.PortOnePaymentResponse;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PortOneClient portOneClient;


    //  결제 확정 메서드
    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        // 1. orderId로 Payment 조회
        Order order = orderRepository.findById(request.orderId()).orElseThrow(
                ()-> new PaymentException(ErrorCode.ORDER_NOT_FOUND)
        );
        Payment payment = paymentRepository.findByOrder(order).orElseThrow(
                ()-> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND)
        );
        // 2. 중복 요청 검증
        if(paymentRepository.existsByPortOneIdAndPaidStatus(request.portOneId(), PaidStatus.SUCCESS)){
            throw new PaymentException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }
        // 3. PortOne 조회 API 호출
        PortOnePaymentResponse portOnePaymentResponse = portOneClient.getPayment(request.portOneId());
        // 3-1. PortOne 결제 상태 검증
        if(PortOnePayStatus.PAID != portOnePaymentResponse.status()){
            throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
        }
        // 4. 금액 검증
        if(portOnePaymentResponse.amount().total() != payment.getPaidAmount()){
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        } // 클라이언트가 보내온 request.payAmount()는 신뢰할 수 없는 값
        // 5. 재고 차감
        for (OrderItem orderItem : order.getOrderItems()) {
            orderItem.getProduct().decreaseStock(orderItem.getQuantity());
        }
        // 6. 최종 확정
        payment.confirmPayment(request.portOneId(), portOnePaymentResponse.paidAt()); // 상태 변경
        order.updateOrderStatus(OrderStatus.PAID); // 주문 상태 성공으로 변경
        paymentRepository.save(payment);

        return new PaymentConfirmResponse(order.getOrderNumber(), "결제가 완료되었습니다.");
    } // Todo: 보상 트랜잭션 추가 해야함
}