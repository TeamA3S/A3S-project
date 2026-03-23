package com.example.a3sproject.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 0 * * *")
    public void processScheduledBillings() {
        log.info("정기결제 스케줄러 시작");
        subscriptionService.processScheduledBillings();
        log.info("정기결제 스케줄러 완료");
    }
}
