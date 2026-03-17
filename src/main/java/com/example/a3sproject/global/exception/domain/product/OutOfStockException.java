package com.example.a3sproject.global.exception.domain.product;

import com.example.a3sproject.global.exception.common.ServiceException;

import static com.example.a3sproject.global.exception.common.ErrorCode.PRODUCT_OUT_OF_STOCK;

public class OutOfStockException extends ServiceException {
    public OutOfStockException() {
        super(PRODUCT_OUT_OF_STOCK);
    }
}
