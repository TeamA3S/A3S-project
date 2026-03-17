package com.example.a3sproject.domain.product.service;

import com.example.a3sproject.domain.product.dto.GetAllProductsResponseDto;
import com.example.a3sproject.domain.product.entity.ProductCategory;
import com.example.a3sproject.domain.product.entity.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    public Page<GetAllProductsResponseDto> getAllProducts(int page, int size, String productName, ProductStatus productStatus, ProductCategory productCategory) {
        Pageable pageable = PageRequest.of(page, size);
        // TODO: QueryDsl로 해결 예정
        return null;
    }
}
