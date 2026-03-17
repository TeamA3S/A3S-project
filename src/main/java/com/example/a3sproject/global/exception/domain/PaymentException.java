package com.example.a3sproject.global.exception.domain;

import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.common.ServiceException;

public class PaymentException extends ServiceException {
    public PaymentException(ErrorCode errorCode) {
        super(errorCode);
    }
}
