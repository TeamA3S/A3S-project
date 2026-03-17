package com.example.a3sproject.global.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
public class SuccessDto<T> implements ApiResponseDto<T> {

    private final int status;  // HttpStatus → int 변환
    private final T data;

    public SuccessDto(HttpStatus status, T data) {
        this.status = status.value();  // 200, 201 등 숫자로 저장
        this.data = data;
    }

    // 데이터 없는 성공 응답용 (예: 로그아웃, 삭제)
    public SuccessDto(HttpStatus status) {
        this.status = status.value();
        this.data = null;
    }
}
