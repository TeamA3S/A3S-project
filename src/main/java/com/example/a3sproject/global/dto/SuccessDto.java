package com.example.a3sproject.global.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public class SuccessDto<T> implements ApiResponseDto<T> {
    private final HttpStatus status;
    T data;

    public SuccessDto(HttpStatus status, T data) {
        this.status = status;
        this.data = data;
    }
}
