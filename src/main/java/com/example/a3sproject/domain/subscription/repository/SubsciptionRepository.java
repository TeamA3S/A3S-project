package com.example.a3sproject.domain.subscription.repository;

import com.example.a3sproject.domain.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubsciptionRepository extends JpaRepository<Subscription, Long> {
}
