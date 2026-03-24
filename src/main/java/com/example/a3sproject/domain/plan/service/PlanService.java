package com.example.a3sproject.domain.plan.service;

import com.example.a3sproject.domain.plan.dto.GetPlanResponseDto;
import com.example.a3sproject.domain.plan.entity.Plan;
import com.example.a3sproject.domain.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
                        plan.getPlanUuid(),
                        plan.getName(),
                        plan.getAmount(),
                        plan.getBillingCycle()
                )).toList();
    }
}
