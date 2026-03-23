package com.example.a3sproject.domain.subscription.repository;

import com.example.a3sproject.domain.subscription.entity.Subscription;
import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionBillingRepository extends JpaRepository<SubscriptionBilling, Long> {



    List<SubscriptionBilling> findBySubscription(Subscription subscription);
}
