package com.example.a3sproject.domain.product.controller;

import com.example.a3sproject.domain.product.dto.GetAllProductsResponseDto;
import com.example.a3sproject.domain.product.dto.GetOneProductResponseDto;
import com.example.a3sproject.domain.product.service.ProductService;
import com.example.a3sproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    // 상품 목록 조회
    @GetMapping("/products")
    public ResponseEntity<ApiResponseDto<Page<GetAllProductsResponseDto>>> getAllProducts(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, productService.getAllProducts(page - 1, size)));
    }

    // 상품 단건 조회
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponseDto<GetOneProductResponseDto>> getOneProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, productService.getOneProduct(productId)));
    }

}
