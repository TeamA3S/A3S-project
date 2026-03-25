package com.example.a3sproject.domain.membership.controller;

import com.example.a3sproject.domain.membership.dto.MyMembershipResponseDto;
import com.example.a3sproject.domain.membership.service.MembershipService;
import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    // 내 멤버십 등급 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<MyMembershipResponseDto>> getMyMembership(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        MyMembershipResponseDto response = membershipService.getMyMembership(userDetails.getUser());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, response));
    }
}
