package com.example.a3sproject.global.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorDto<T> implements ApiResponseDto<T> {

    private final String code;
    private final String message;
    private final T data;  // 에러 상세 데이터 (보통 null)
}