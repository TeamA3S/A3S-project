package com.example.a3sproject.global.exception.domain;

import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.common.ServiceException;

public class SubscriptionException extends ServiceException {
    public  SubscriptionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
