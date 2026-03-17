package com.example.a3sproject.global.controller;

import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.dto.LoginRequestDto;
import com.example.a3sproject.global.dto.LoginResponseDto;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.UserException;
import com.example.a3sproject.global.security.JwtTokenProvider;
import com.example.a3sproject.global.security.refreshtoken.entity.RefreshToken;
import com.example.a3sproject.global.security.refreshtoken.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 로그인 API
     * POST /api/auth/login
     * - Access Token → Response Header (Authorization: Bearer {token})
     * - Refresh Token → Response Body
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @RequestBody LoginRequestDto request) {

        // 1. 인증 시도 — 실패 시 BadCredentialsException → GlobalExceptionHandler 위임
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new UserException(ErrorCode.USER_UNAUTHORIZED);
        }

        // 2. Access Token 발급
        String accessToken = jwtTokenProvider.createToken(request.getEmail());

        // 3. Refresh Token 발급 및 DB 저장 (Rotation: 기존 토큰 교체)
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(request.getEmail());

        refreshTokenRepository.findByEmail(request.getEmail())
                .ifPresentOrElse(
                        existing -> existing.rotate(
                                refreshTokenValue,
                                jwtTokenProvider.getRefreshTokenExpiresAt()
                        ),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .email(request.getEmail())
                                        .token(refreshTokenValue)
                                        .expiresAt(jwtTokenProvider.getRefreshTokenExpiresAt())
                                        .build()
                        )
                );

        // 4. 응답
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + accessToken)
                .body(ApiResponseDto.success(
                        HttpStatus.OK,
                        LoginResponseDto.builder()
                                .refreshToken(refreshTokenValue)
                                .email(request.getEmail())
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

        String refreshTokenValue = request.getRefreshToken();

        // 1. DB에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UserException(ErrorCode.USER_UNAUTHORIZED));

        // 2. 만료 여부 확인
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UserException(ErrorCode.USER_UNAUTHORIZED);
        }

        // 3. Access Token 재발급
        String newAccessToken = jwtTokenProvider.createToken(refreshToken.getEmail());

        // 4. Refresh Token Rotation
        String newRefreshToken = jwtTokenProvider.createRefreshToken(refreshToken.getEmail());
        refreshToken.rotate(newRefreshToken, jwtTokenProvider.getRefreshTokenExpiresAt());

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + newAccessToken)
                .body(ApiResponseDto.success(
                        HttpStatus.OK,
                        LoginResponseDto.builder()
                                .refreshToken(newRefreshToken)
                                .email(refreshToken.getEmail())
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

    /**
     * 현재 로그인한 사용자 정보 조회 API
     * GET /api/auth/me
     * TODO: 호정님 UserService 완성 후 DB 연동
     * 응답:
     * {
     *   "success": true,
     *   "email": "user@example.com",
     *   "customerUid": "CUST_xxxxx",
     *   "name": "홍길동"
     * }
     *
     * 중요: customerUid는 PortOne 빌링키 발급 시 활용!
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Principal principal) {

        String email = principal.getName();

        // TODO: 구현
        // 데이터베이스에서 사용자 정보 조회
        // customerUid 생성은 조회 한 사용자 정보로 조합하여 생성, 추천 조합 : CUST_{userId}_{rand6:난수}
        // 임시 구현
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("email", email);
        response.put("customerUid", "CUST_" + Math.abs(email.hashCode()));  // PortOne 고객 UID
        response.put("name", email.split("@")[0]);  // 이메일에서 이름 추출
        response.put("phone", "010-0000-0000");  // Kg 이니시스 전화번호 필수
        response.put("pointBalance", 1000L);  // 포인트 잔액

        return ResponseEntity.ok(response);
    }
}
