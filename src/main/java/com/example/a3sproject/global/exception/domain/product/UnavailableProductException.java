package com.example.a3sproject.global.exception.domain.product;

import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.common.ServiceException;

public class UnavailableProductException extends ServiceException {
    public UnavailableProductException() {
        super(ErrorCode.ORDERITEM_UNAVAILABLE);
    }
}
