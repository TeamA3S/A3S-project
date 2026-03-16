package com.example.a3sproject.global.dto;


import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

public interface ApiResponseDto<T> {

    static <T> ApiResponseDto<T> success(HttpStatus status, T data) {
        return new SuccessDto<>(status, data);
    }
    static <T> ApiResponseDto<T> successWithNoContent() {
        return new SuccessDto<>(HttpStatus.NO_CONTENT);
    }

    static <T> ApiResponseDto<T> pagination(HttpStatus status,Page<T> page , String message) {
        return new PageResponseDto<>(
                status,
                page.getNumber(),
                page.getSize(),
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                message
                );
    }

    static <T> ApiResponseDto<T> error(String message) {
        return new ErrorDto<>(message, null);
    }

    static <T> ApiResponseDto<T> errorWithMap(T map, String message) {
        return new ErrorDto<>(message, map);
    }
}
