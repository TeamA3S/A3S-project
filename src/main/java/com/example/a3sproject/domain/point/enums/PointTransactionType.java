package com.example.a3sproject.domain.point.enums;

public enum PointTransactionType {
    EARN,    // 적립
    USE,     // 사용
    RESTORE, // 복구 (결제 실패 시)
    CANCEL,  // 취소 (환불)
    EXPIRE,  // 소멸
    ADMIN    // 관리자 조정
}
