package com.example.a3sproject.domain.product.controller;

import com.example.a3sproject.domain.product.dto.GetAllProductsResponseDto;
import com.example.a3sproject.domain.product.entity.ProductCategory;
import com.example.a3sproject.domain.product.entity.ProductStatus;
import com.example.a3sproject.domain.product.service.ProductService;
import com.example.a3sproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 목록 조회
    @GetMapping("/products")
    public ResponseEntity<ApiResponseDto<Page<GetAllProductsResponseDto>>> getAllProducts(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) ProductStatus productStatus,
            @RequestParam(required = false) ProductCategory productCategory
    ) {
        Page<GetAllProductsResponseDto> productsPage =
                productService.getAllProducts(page - 1, size, productName, productStatus, productCategory);
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, productsPage));
    }

    /**
     * TODO : 상품 단건 조회
     * 1. 메서드명은 무엇일지?
     * 2. 엔트포인트는 어떻게 될지?
     * 3. 반환 타입은 무엇이며 어떤 것을 반환할지?
     * 4. 파라미터로 어떤 값을 받아서 조회할지?
     * 5. 성공적으로 조회된다면 어떤 상태코드를 반환할지?
     * 6. productService에서는 어떤 메서드를 생성하고 구현해야할지? (인수는 어떤 걸 받아야할지?)
     */

}
