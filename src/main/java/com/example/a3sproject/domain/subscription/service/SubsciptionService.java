package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.domain.subscription.repository.SubsciptionBillingRepository;
import com.example.a3sproject.domain.subscription.repository.SubsciptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubsciptionService {
    private final SubsciptionRepository subsciptionRepository;
    private final SubsciptionBillingRepository subsciptionBillingRepository;


}
