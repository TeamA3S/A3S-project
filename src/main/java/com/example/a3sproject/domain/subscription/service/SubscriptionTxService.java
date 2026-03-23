package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// SubscriptionTxService (트랜잭션 있음)
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionTxService {
//    private final UserRepository userRepository;

//    public CreateSubscriptionResponse saveSubscription(long userId, CreateSubscriptionRequest request) {
//        User user = userRepository.findById(userId).orElseThrow(
//                () -> new
//        );
//
//        // PaymentMethod 저장
//        PaymentMethod paymentMethod = new PaymentMethod(
//
//        )
//        // Subscription 저장
//    }
}