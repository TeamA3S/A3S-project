package com.example.a3sproject.domain.user.controller;

import com.example.a3sproject.domain.user.dto.SignupUserRequest;
import com.example.a3sproject.domain.user.dto.SignupUserResponse;
import com.example.a3sproject.domain.user.dto.UserProfileResponseDto;
import com.example.a3sproject.domain.user.service.UserService;
import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/signUp")
    public ResponseEntity<ApiResponseDto<SignupUserResponse>> createUser(
            @Valid @RequestBody SignupUserRequest request
    ) {
        SignupUserResponse response = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, response));
    }

    // 내 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        UserProfileResponseDto response = userService.getMyProfile(userDetails.getEmail());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, response));
    }
}
