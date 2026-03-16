package com.example.a3sproject.global.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PageResponseDto<T> implements ApiResponseDto<T>  {
    private final HttpStatus status;
    private final int page;
    private final int size;
    private final List<T> items;
    private final int totalPages;
    private final Long totalElements;
    private final String message;
}