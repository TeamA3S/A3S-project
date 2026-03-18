package com.example.a3sproject.domain.refunds.service;

import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.domain.payment.repository.PaymentRepository;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentResponse;
import com.example.a3sproject.domain.refunds.dto.request.RefundRequest;
import com.example.a3sproject.domain.refunds.entity.Refund;
import com.example.a3sproject.domain.refunds.enums.RefundStatus;
import com.example.a3sproject.domain.refunds.repository.RefundRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.RefundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;

//    public void refundPayment(RefundRequest request) {
//        // 1. paymentId로 Payment 조회 (존재 여부 확인)
//        Payment payment = paymentRepository.findById(request.paymentId()).orElseThrow(
//                () -> new RefundException(ErrorCode.PAYMENT_NOT_FOUND)
//        );
//        // 2. 결제 상태가 SUCCESS인지 확인
//        if(!payment.getPaidStatus().equals(PaidStatus.SUCCESS)){
//            throw new RefundException(ErrorCode.PAYMENT_NOT_SUCCESS);
//        }
//        // 3. 이미 환불된 내역이 있는지 확인 (멱등성)
//        if(refundRepository.existsByPaymentIdAndRefundStatus(request.paymentId(), RefundStatus.COMPLETED)){
//            throw new RefundException(ErrorCode.DUPLICATE_REFUND_REQUEST);
//        }
//        // 4. Refund 엔티티 생성 (REQUEST 상태)
//        Refund refund = new Refund(payment, request.reason());
//
//        // 5. PortOne 결제 취소 API 호출
//        PortOneCancelPaymentResponse cancelResponse = portOneClient.cancelPayment(payment.getPaymentUuid(), request);
//        // 6. status 검증
//
//
//        // 7. 성공 → completeRefund() + 결제/주문 상태 변경
//
//
//        // 8. 실패 → cancelRefund()
//
//    }
}
