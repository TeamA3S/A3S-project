package com.example.a3sproject.global.exception.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 유저입니다."),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    USER_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Point
    POINT_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "포인트 잔액이 부족합니다."),
    POINT_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 포인트 금액입니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_PAID(HttpStatus.CONFLICT, "이미 결제된 주문입니다."),
    ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "취소할 수 없는 주문 상태입니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 주문상태입니다."),

    // OrderItem
    ORDERITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "주문 상품을 찾을 수 없습니다."),
    ORDERITEM_UNAVAILABLE(HttpStatus.BAD_REQUEST, "주문할 수 없는 상품입니다."),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 결제입니다."),
    PAYMENT_PORTONE_ERROR(HttpStatus.BAD_GATEWAY, "PortOne 결제 처리 중 오류가 발생했습니다."),
    DUPLICATE_PAYMENT_REQUEST(HttpStatus.CONFLICT, "이미 처리된 결제 요청입니다."),
    PAYMENT_NOT_SUCCESS(HttpStatus.BAD_REQUEST, "완료되지 않은 결제입니다."),

    // Refund
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "환불 정보를 찾을 수 없습니다."),
    ORDER_CANNOT_REFUND(HttpStatus.CONFLICT, "환불 가능한 상태가 아닙니다."),
    DUPLICATE_REFUND_REQUEST(HttpStatus.CONFLICT, "이미 처리된 환불 요청입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_OUT_OF_STOCK(HttpStatus.CONFLICT, "상품 재고가 부족합니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
