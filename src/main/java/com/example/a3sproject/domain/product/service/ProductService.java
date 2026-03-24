package com.example.a3sproject.domain.product.service;

import com.example.a3sproject.domain.product.dto.GetAllProductsResponseDto;
import com.example.a3sproject.domain.product.dto.GetOneProductResponseDto;
import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // 상품 목록 조회
    public List<GetAllProductsResponseDto> getAllProducts() {
        return productRepository.findAllByProductStatus(ProductStatus.ON_SALE)
                .stream()
                .map(product -> new GetAllProductsResponseDto(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getStock()
                )).toList();
    }

    // 상품 단건 조회
    public GetOneProductResponseDto getOneProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND)
        );
        return new GetOneProductResponseDto(product);
    }
}
