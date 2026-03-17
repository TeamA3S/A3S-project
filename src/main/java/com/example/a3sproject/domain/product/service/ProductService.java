package com.example.a3sproject.domain.product.service;

import com.example.a3sproject.domain.product.dto.GetAllProductsResponseDto;
import com.example.a3sproject.domain.product.dto.GetOneProductResponseDto;
import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import com.example.a3sproject.global.exception.domain.product.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // 상품 목록 조회
    public Page<GetAllProductsResponseDto> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAllByProductStatus(ProductStatus.ON_SALE, pageable)
                .map(product ->
                        new GetAllProductsResponseDto(
                                product.getId(), product.getName(), product.getPrice(), product.getStock())
                );
    }

    // 상품 단건 조회
    public GetOneProductResponseDto getOneProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(
                ProductNotFoundException::new
        );
        return new GetOneProductResponseDto(product);
    }
}
