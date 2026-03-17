package com.example.a3sproject.global.exception.domain;

import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.common.ServiceException;

public class UserException extends ServiceException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
