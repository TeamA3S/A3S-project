package com.example.a3sproject.domain.plan.controller;

import com.example.a3sproject.domain.plan.dto.GetPlanResponseDto;
import com.example.a3sproject.domain.plan.service.PlanService;
import com.example.a3sproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    // 플랜 조회
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<GetPlanResponseDto>>> getPlans() {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, planService.getPlans()));
    }
}
