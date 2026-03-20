package com.example.a3sproject.domain.plan.service;

import com.example.a3sproject.domain.plan.controller.PlanController;
import com.example.a3sproject.domain.plan.dto.GetPlanResponseDto;
import com.example.a3sproject.domain.plan.entity.Plan;
import com.example.a3sproject.domain.plan.repository.PlanRepository;
import com.example.a3sproject.domain.product.dto.GetAllProductsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    // 플랜 조회
    public List<GetPlanResponseDto> getPlans() {
        return planRepository.findAll()
                .stream()
                .filter(Plan::isActive)
                .map(plan -> new GetPlanResponseDto(
                        String.valueOf(plan.getId()),
                        plan.getName(),
                        plan.getAmount(),
                        plan.getBillingCycle()
                )).toList();
    }
}
