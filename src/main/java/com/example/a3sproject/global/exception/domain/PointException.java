package com.example.a3sproject.global.exception.domain;

import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.common.ServiceException;

public class PointException extends ServiceException {
    public PointException(ErrorCode errorCode) {
        super(errorCode);
    }
}
