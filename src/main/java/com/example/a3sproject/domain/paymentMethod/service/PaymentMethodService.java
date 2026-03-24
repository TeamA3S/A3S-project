package com.example.a3sproject.domain.paymentMethod.service;

import com.example.a3sproject.domain.paymentMethod.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

}
