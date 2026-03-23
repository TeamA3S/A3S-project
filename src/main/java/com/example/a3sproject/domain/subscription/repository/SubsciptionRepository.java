package com.example.a3sproject.domain.subscription.repository;

import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubsciptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findBySubscriptionUuid(String subscriptionId);

    boolean existsByUserIdAndPlanIdAndStatus(long userId, String planId, SubscriptionStatus subscriptionStatus);
}
