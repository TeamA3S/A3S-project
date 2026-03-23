package com.example.a3sproject.global.controller;

import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.dto.LoginRequestDto;
import com.example.a3sproject.global.dto.LoginResponseDto;
import com.example.a3sproject.global.security.refreshtoken.repository.RefreshTokenRepository;
import com.example.a3sproject.global.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthService authService;

    /**
     * 로그인 API
     * POST /api/auth/login
     * - Access Token → Response Header (Authorization: Bearer {token})
     * - Refresh Token → Response Body
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {

        AuthService.AuthTokenDto tokens = authService.login(request);

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(ApiResponseDto.success(
                        HttpStatus.OK,
                        LoginResponseDto.builder()
                                .refreshToken(tokens.refreshToken())
                                .email(tokens.email())
                                .build()
                ));
    }

    /**
     * 토큰 재발급 API
     * POST /api/auth/reissue
     */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> reissue(
            @RequestBody LoginResponseDto request) {

        AuthService.AuthTokenDto tokens = authService.reissue(request.getRefreshToken());

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(ApiResponseDto.success(
                        HttpStatus.OK,
                        LoginResponseDto.builder()
                                .refreshToken(tokens.refreshToken())
                                .email(tokens.email())
                                .build()
                ));
    }

    /**
     * 로그아웃 API
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(Principal principal) {
        refreshTokenRepository.deleteByEmail(principal.getName());
        return ResponseEntity.ok()
                .body(ApiResponseDto.successWithNoContent());
    }
}
