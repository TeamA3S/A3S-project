package com.example.a3sproject.domain.subscription.repository;

import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByStatusInAndCurrentPeriodEndBefore(
            List<SubscriptionStatus> statuses,
            OffsetDateTime now
    );
    Optional<Subscription> findBySubscriptionUuid(String subscriptionId);

    boolean existsByUserIdAndPlanIdAndStatus(long userId, String planId, SubscriptionStatus subscriptionStatus);

    // 본인 구독인지 확인 메서드용
    Optional<Subscription> findBySubscriptionUuidAndUser_Id(String subscriptionUuid, Long userId);
}
