package com.example.a3sproject.global.exception.domain.product;

import com.example.a3sproject.global.exception.common.ServiceException;

import static com.example.a3sproject.global.exception.common.ErrorCode.PRODUCT_NOT_FOUND;

public class ProductNotFoundException extends ServiceException {
    public ProductNotFoundException() {
        super(PRODUCT_NOT_FOUND);
    }
}
