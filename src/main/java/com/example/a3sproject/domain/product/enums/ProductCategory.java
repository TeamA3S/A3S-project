package com.example.a3sproject.domain.product.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductCategory {

    UPSTREAM("상류"),
    MIDSTREAM("중류"),
    DOWNSTREAM("하류");

    private final String productCategory;
}