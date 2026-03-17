package com.example.a3sproject.global.exception.domain;

import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.common.ServiceException;

public class OrderException extends ServiceException {
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
}
