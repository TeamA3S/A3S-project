package com.example.a3sproject.global.controller;

import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.dto.LoginRequestDto;
import com.example.a3sproject.global.dto.LoginResponseDto;
import com.example.a3sproject.global.security.refreshtoken.repository.RefreshTokenRepository;
import com.example.a3sproject.global.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

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
            @RequestBody LoginRequestDto request) {

        AuthService.AuthTokenDto tokens = authService.login(request);

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(ApiResponseDto.success(
                        HttpStatus.OK,
                        new LoginResponseDto(tokens.refreshToken(), tokens.email())
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
                        new LoginResponseDto(tokens.refreshToken(), tokens.email())
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
