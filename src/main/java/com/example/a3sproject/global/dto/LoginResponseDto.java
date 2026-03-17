package com.example.a3sproject.global.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDto {
    private final String refreshToken;
    private final String email;
}