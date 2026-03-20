package com.example.a3sproject.domain.paymentMethod.controller;

import com.example.a3sproject.domain.paymentMethod.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;
}
