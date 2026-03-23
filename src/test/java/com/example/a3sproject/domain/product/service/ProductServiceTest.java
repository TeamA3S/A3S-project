package com.example.a3sproject.domain.product.service;

import com.example.a3sproject.domain.product.dto.GetAllProductsResponseDto;
import com.example.a3sproject.domain.product.dto.GetOneProductResponseDto;
import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.enums.ProductCategory;
import com.example.a3sproject.domain.product.enums.ProductStatus;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private final Product 판매중_상품 = createProduct(1L, "테스트 상품A", 10000, 50, "설명A", ProductStatus.ON_SALE, ProductCategory.UPSTREAM);
    private final Product 품절_상품 = createProduct(2L, "테스트 상품B", 20000, 0, "설명B", ProductStatus.SOLD_OUT, ProductCategory.MIDSTREAM);
    private final Product 단종_상품 = createProduct(3L, "테스트 상품C", 30000, 0, "설명C", ProductStatus.DISCONTINUED, ProductCategory.DOWNSTREAM);

    private Product createProduct(Long id, String name, int price, int stock, String description, ProductStatus status, ProductCategory category) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .stock(stock)
                .description(description)
                .productStatus(status)
                .productCategory(category)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @Test
    @DisplayName("상품 목록을 조회하면 판매 중인 상품만 DTO 리스트로 반환된다")
    void getAllProducts_판매중상품존재_목록반환() {
        // given: ON_SALE 상태의 상품만 Repository에서 반환되도록 설정
        given(productRepository.findAllByProductStatus(ProductStatus.ON_SALE)).willReturn(List.of(판매중_상품));

        // when
        List<GetAllProductsResponseDto> result = productService.getAllProducts();

        // then: 반환된 리스트에 판매 중인 상품만 포함되며 모든 필드 매핑이 정확하다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("테스트 상품A");
        assertThat(result.get(0).getPrice()).isEqualTo(10000);
        assertThat(result.get(0).getStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("SOLD_OUT 상태의 상품은 상품 목록 조회 결과에 포함되지 않는다")
    void getAllProducts_품절상품존재_품절상품제외됨() {
        // given: Repository가 ON_SALE 필터 기준으로 판매중 상품만 반환하는 상황
        given(productRepository.findAllByProductStatus(ProductStatus.ON_SALE)).willReturn(List.of(판매중_상품));

        // when
        List<GetAllProductsResponseDto> result = productService.getAllProducts();

        // then: 결과 목록에 SOLD_OUT 상품의 이름이 포함되지 않는다
        assertThat(result).hasSize(1);
        assertThat(result).extracting(GetAllProductsResponseDto::getName)
                .doesNotContain("테스트 상품B");
    }

    @Test
    @DisplayName("SOLD_OUT과 DISCONTINUED 상품이 존재해도 ON_SALE 상품만 반환한다")
    void getAllProducts_다양한상태상품혼재_판매중만반환() {
        // given: ON_SALE 필터로 조회 시 판매중 상품 하나만 반환되는 상황
        given(productRepository.findAllByProductStatus(ProductStatus.ON_SALE)).willReturn(List.of(판매중_상품));

        // when
        List<GetAllProductsResponseDto> result = productService.getAllProducts();

        // then: 결과에 SOLD_OUT, DISCONTINUED 상품이 모두 제외되고 ON_SALE 상품만 담긴다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트 상품A");
    }

    @Test
    @DisplayName("판매 중인 상품이 없으면 빈 리스트를 반환한다")
    void getAllProducts_판매중상품없음_빈리스트반환() {
        // given: ON_SALE 상품이 하나도 없는 상황
        given(productRepository.findAllByProductStatus(ProductStatus.ON_SALE)).willReturn(List.of());

        // when
        List<GetAllProductsResponseDto> result = productService.getAllProducts();

        // then: 예외 없이 빈 리스트가 반환된다
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하는 상품 ID로 단건 조회하면 해당 상품의 상세 정보가 반환된다")
    void getOneProduct_존재하는ID_상세정보반환() {
        // given: 특정 ID로 상품 단건 조회 성공
        given(productRepository.findById(1L)).willReturn(Optional.of(판매중_상품));

        // when
        GetOneProductResponseDto result = productService.getOneProduct(1L);

        // then: 상품의 모든 필드 값이 DTO에 누락 없이 매핑된다
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("테스트 상품A");
        assertThat(result.getPrice()).isEqualTo(10000);
        assertThat(result.getStock()).isEqualTo(50);
        assertThat(result.getDescription()).isEqualTo("설명A");
        assertThat(result.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(result.getCategory()).isEqualTo(ProductCategory.UPSTREAM);
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void getOneProduct_존재하지않는ID_PRODUCT_NOT_FOUND() {
        // given: 조회 결과가 없는 상품 ID
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        // when & then: PRODUCT_NOT_FOUND 에러코드를 가진 ProductException이 발생한다
        assertThatThrownBy(() -> productService.getOneProduct(999L))
                .isInstanceOf(ProductException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("SOLD_OUT 상태의 상품도 ID로 단건 조회는 가능하다")
    void getOneProduct_품절상품ID_상세정보반환() {
        // given: SOLD_OUT 상태인 상품이 존재하는 상황
        given(productRepository.findById(2L)).willReturn(Optional.of(품절_상품));

        // when
        GetOneProductResponseDto result = productService.getOneProduct(2L);

        // then: 상품 상태에 무관하게 단건 조회는 성공하며 SOLD_OUT 상태가 그대로 반환된다
        assertThat(result.getProductId()).isEqualTo(2L);
        assertThat(result.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        assertThat(result.getStock()).isEqualTo(0);
    }

    // -------------------------------------------------------
    // Product Entity 도메인 로직 단위 테스트 (Mock 불필요)
    // -------------------------------------------------------

    @Test
    @DisplayName("상품 재고를 차감하면 기존 재고에서 요청 수량만큼 감소한다")
    void decreaseStock_정상수량_재고차감성공() {
        // given: 재고가 50개인 상품
        Product product = createProduct(10L, "재고 테스트 상품", 1000, 50, "설명", ProductStatus.ON_SALE, ProductCategory.DOWNSTREAM);

        // when
        product.decreaseStock(10);

        // then: 50 - 10 = 40으로 재고가 감소한다
        assertThat(product.getStock()).isEqualTo(40);
    }

    @Test
    @DisplayName("재고와 동일한 수량을 차감하면 재고가 0이 된다")
    void decreaseStock_재고전량차감_재고0() {
        // given: 재고가 5개인 상품
        Product product = createProduct(11L, "전량 차감 상품", 1000, 5, "설명", ProductStatus.ON_SALE, ProductCategory.DOWNSTREAM);

        // when: 재고 전량 차감
        product.decreaseStock(5);

        // then: 재고가 0이 된다
        assertThat(product.getStock()).isZero();
    }

    @Test
    @DisplayName("현재 재고보다 많은 수량을 차감하려 하면 PRODUCT_OUT_OF_STOCK 예외가 발생한다")
    void decreaseStock_재고초과차감_PRODUCT_OUT_OF_STOCK() {
        // given: 재고가 4개인 상품에 5개 차감 요청
        Product product = createProduct(12L, "재고 부족 상품", 1000, 4, "설명", ProductStatus.ON_SALE, ProductCategory.DOWNSTREAM);

        // when & then: 재고 부족으로 PRODUCT_OUT_OF_STOCK 예외가 발생한다
        assertThatThrownBy(() -> product.decreaseStock(5))
                .isInstanceOf(ProductException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_OUT_OF_STOCK);
    }

    @Test
    @DisplayName("재고가 0인 상품에서 1 이상 차감을 시도하면 PRODUCT_OUT_OF_STOCK 예외가 발생한다")
    void decreaseStock_재고0에서차감시도_PRODUCT_OUT_OF_STOCK() {
        // given: 재고가 이미 0인 품절 상태 상품
        Product product = createProduct(13L, "재고 없는 상품", 1000, 0, "설명", ProductStatus.SOLD_OUT, ProductCategory.DOWNSTREAM);

        // when & then: 재고가 0인 상태에서 차감 시도 시 예외가 발생한다
        assertThatThrownBy(() -> product.decreaseStock(1))
                .isInstanceOf(ProductException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_OUT_OF_STOCK);
    }

    @Test
    @DisplayName("0 수량을 차감하더라도 재고는 변하지 않는다")
    void decreaseStock_0수량차감_재고변화없음() {
        // given: 재고가 10개인 상품에 0 차감 요청
        Product product = createProduct(14L, "0차감 상품", 1000, 10, "설명", ProductStatus.ON_SALE, ProductCategory.DOWNSTREAM);

        // when: 0 수량 차감 (결제 흐름 중 포인트 전액 결제 시 발생할 수 있는 케이스)
        product.decreaseStock(0);

        // then: 재고가 그대로 10개 유지된다
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("상품 재고를 증가시키면 기존 재고에서 요청 수량만큼 증가한다")
    void increaseStock_정상수량_재고증가성공() {
        // given: 재고가 10개인 상품에 5개 환불/복구 요청
        Product product = createProduct(15L, "재고 복구 상품", 1000, 10, "설명", ProductStatus.ON_SALE, ProductCategory.DOWNSTREAM);

        // when
        product.increaseStock(5);

        // then: 10 + 5 = 15로 재고가 증가한다
        assertThat(product.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("재고가 0인 상품에 재고를 추가하면 해당 수량만큼 재고가 생긴다")
    void increaseStock_재고0에서증가_재고복구성공() {
        // given: 재고가 0인 품절 상품에 환불로 인한 재고 복구
        Product product = createProduct(16L, "재고 복구 품절상품", 1000, 0, "설명", ProductStatus.SOLD_OUT, ProductCategory.DOWNSTREAM);

        // when
        product.increaseStock(3);

        // then: 재고가 3으로 복구된다
        assertThat(product.getStock()).isEqualTo(3);
    }
}