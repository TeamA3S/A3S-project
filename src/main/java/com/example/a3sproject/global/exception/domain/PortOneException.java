package com.example.a3sproject.global.exception.domain;

import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.common.ServiceException;

public class PortOneException extends ServiceException {
    public PortOneException(ErrorCode errorCode) {
        super(errorCode);
    }
}
