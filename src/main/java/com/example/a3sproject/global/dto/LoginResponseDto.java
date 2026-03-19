package com.example.a3sproject.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponseDto {
    private final boolean success;
    private final String refreshToken;
    private final String email;
}