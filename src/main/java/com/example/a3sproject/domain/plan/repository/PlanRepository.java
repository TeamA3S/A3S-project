package com.example.a3sproject.domain.plan.repository;

import com.example.a3sproject.domain.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByPlanUuid(String planUuid);
}
