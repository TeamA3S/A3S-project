package com.example.a3sproject.global.common;

/**
 * 프로젝트 전역에서 사용하는 상수를 관리하는 클래스
 */
public final class AppConstants {
    private AppConstants() {} // 인스턴스화 방지

    public static final class Point {
        public static final int EXPIRATION_YEARS = 1;        // 포인트 만료 기간 (년)
        public static final int BATCH_CHUNK_SIZE = 10;       // 스케줄러 배치 크기
        public static final int BATCH_MAX_RECORDS = 100;     // 스케줄러 최대 처리 건수
        public static final long BATCH_DELAY_MS = 1000L;     // 배치 간 지연 시간
    }

    public static final class Order {
        public static final String NUMBER_PREFIX = "ODN-";   // 주문번호 접두사
        public static final int NUMBER_RANDOM_LENGTH = 10;   // 주문번호 랜덤 문자열 길이
    }

    public static final class Payment {
        public static final String NUMBER_PREFIX = "PMN-";   // 결제번호 접두사
    }

    public static final class Security {
        public static final long CORS_MAX_AGE = 3600L;       // CORS 캐싱 시간 (1시간)
    }
}
