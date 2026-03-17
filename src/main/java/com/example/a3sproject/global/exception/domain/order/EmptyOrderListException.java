package com.example.a3sproject.global.exception.domain.order;

import com.example.a3sproject.global.exception.common.ServiceException;

import static com.example.a3sproject.global.exception.common.ErrorCode.ORDERITEM_NOT_FOUND;

public class EmptyOrderListException extends ServiceException {
    public EmptyOrderListException() {
        super(ORDERITEM_NOT_FOUND);
    }
}
