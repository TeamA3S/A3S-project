package com.example.a3sproject.domain.product.controller;

import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductCategory;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // 각 테스트 시작 전 DataInitializer로 생성된 더미 데이터 포함, 모든 상품 데이터 초기화
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("상품 목록 조회 API를 호출하면 200 OK와 함께 ON_SALE 상품 목록을 반환한다")
    void getAllProducts_정상요청_200응답() throws Exception {
        // given: ON_SALE 상품 1개, SOLD_OUT 상품 1개를 DB에 저장
        Product onSaleProduct = Product.builder()
                .name("상품A")
                .price(10000)
                .stock(50)
                .description("설명A")
                .productStatus(ProductStatus.ON_SALE)
                .productCategory(ProductCategory.UPSTREAM)
                .build();
        Product soldOutProduct = Product.builder()
                .name("품절상품B")
                .price(20000)
                .stock(0)
                .description("설명B")
                .productStatus(ProductStatus.SOLD_OUT)
                .productCategory(ProductCategory.MIDSTREAM)
                .build();
        productRepository.save(onSaleProduct);
        productRepository.save(soldOutProduct);

        // when
        ResultActions result = mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON));

        // then: 200 반환, ON_SALE 상품만 포함되며 SOLD_OUT 상품은 응답에서 제외된다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("상품A"))
                .andExpect(jsonPath("$.data[0].price").value(10000))
                .andExpect(jsonPath("$.data[0].stock").value(50));
    }

    @Test
    @DisplayName("SOLD_OUT 상품은 상품 목록 API 응답에 포함되지 않는다")
    void getAllProducts_품절상품존재_품절상품미포함() throws Exception {
        // given: ON_SALE 1개, SOLD_OUT 1개, DISCONTINUED 1개를 각각 저장
        productRepository.save(Product.builder()
                .name("판매중상품").price(10000).stock(10).description("판매중")
                .productStatus(ProductStatus.ON_SALE).productCategory(ProductCategory.UPSTREAM).build());
        productRepository.save(Product.builder()
                .name("품절상품").price(5000).stock(0).description("품절")
                .productStatus(ProductStatus.SOLD_OUT).productCategory(ProductCategory.MIDSTREAM).build());
        productRepository.save(Product.builder()
                .name("단종상품").price(3000).stock(0).description("단종")
                .productStatus(ProductStatus.DISCONTINUED).productCategory(ProductCategory.DOWNSTREAM).build());

        // when
        ResultActions result = mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON));

        // then: 응답 배열 크기가 1이고, 품절/단종 상품명이 응답에 포함되지 않는다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("판매중상품"));
    }

    @Test
    @DisplayName("판매 중인 상품이 없을 때 상품 목록 API를 호출하면 200 OK와 빈 배열을 반환한다")
    void getAllProducts_판매중상품없음_200빈배열반환() throws Exception {
        // given: SOLD_OUT 상품만 저장된 상황 (ON_SALE 상품 없음)
        productRepository.save(Product.builder()
                .name("품절상품만").price(5000).stock(0).description("품절")
                .productStatus(ProductStatus.SOLD_OUT).productCategory(ProductCategory.MIDSTREAM).build());

        // when
        ResultActions result = mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON));

        // then: 200 OK와 빈 배열이 반환된다 (404나 500이 아님을 명시적으로 검증)
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("상품 단건 조회 API를 호출하면 200 OK와 해당 상품의 모든 상세 정보를 반환한다")
    void getOneProduct_정상요청_200응답() throws Exception {
        // given: 특정 상품을 DB에 저장
        Product product = Product.builder()
                .name("단건조회상품")
                .price(15000)
                .stock(30)
                .description("상세설명")
                .productStatus(ProductStatus.ON_SALE)
                .productCategory(ProductCategory.DOWNSTREAM)
                .build();
        Product savedProduct = productRepository.save(product);

        // when
        ResultActions result = mockMvc.perform(get("/api/products/{productId}", savedProduct.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // then: 200 반환, 상품의 모든 필드가 응답에 포함된다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(savedProduct.getId()))
                .andExpect(jsonPath("$.data.name").value("단건조회상품"))
                .andExpect(jsonPath("$.data.price").value(15000))
                .andExpect(jsonPath("$.data.stock").value(30))
                .andExpect(jsonPath("$.data.description").value("상세설명"))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.category").value("DOWNSTREAM"));
    }

    @Test
    @DisplayName("SOLD_OUT 상태의 상품도 ID로 단건 조회하면 200 OK와 상세 정보를 반환한다")
    void getOneProduct_품절상품조회_200응답() throws Exception {
        // given: SOLD_OUT 상품이 DB에 존재하는 상황
        Product soldOutProduct = Product.builder()
                .name("품절단건조회").price(20000).stock(0).description("품절상품설명")
                .productStatus(ProductStatus.SOLD_OUT).productCategory(ProductCategory.UPSTREAM).build();
        Product saved = productRepository.save(soldOutProduct);

        // when
        ResultActions result = mockMvc.perform(get("/api/products/{productId}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // then: 목록 조회와 달리 단건 조회는 상태에 무관하게 성공하며 SOLD_OUT 상태가 반환된다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SOLD_OUT"))
                .andExpect(jsonPath("$.data.stock").value(0));
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 단건 조회하면 404와 PRODUCT_NOT_FOUND 에러코드를 반환한다")
    void getOneProduct_존재하지않는ID_404에러응답() throws Exception {
        // given: DB에 존재하지 않는 상품 ID
        Long nonExistentId = 9999L;

        // when
        ResultActions result = mockMvc.perform(get("/api/products/{productId}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 404 상태코드와 PRODUCT_NOT_FOUND 에러코드, 에러 메시지가 반환된다
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
    }
}