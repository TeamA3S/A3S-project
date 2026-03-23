package com.example.a3sproject.domain.subscription.repository;

import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionBillingRepository extends JpaRepository<SubscriptionBilling, Long> {

    // 본인 구독인지 확인 메서드용
    Optional<SubscriptionBilling> findByIdAndUserId(String id, Long userId);

    List<SubscriptionBilling> findBySubscription(Subscription subscription);
}
