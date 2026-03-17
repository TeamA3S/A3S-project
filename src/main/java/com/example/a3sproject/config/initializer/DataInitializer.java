package com.example.a3sproject.config.initializer;

import com.example.a3sproject.domain.product.entity.Product;
import com.example.a3sproject.domain.product.entity.ProductCategory;
import com.example.a3sproject.domain.product.entity.ProductStatus;
import com.example.a3sproject.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (productRepository.count() == 0) {

            Product p1 = Product.builder()
                    .name("북한강 상류 요정의 샘물")
                    .price(5000)
                    .stock(10)
                    .description("팔당댐을 거치지 않은 최상류의 극강의 맑은 물 (VIP 한정)")
                    .productStatus(ProductStatus.ON_SALE)
                    .productCategory(ProductCategory.UPSTREAM)
                    .build();

            Product p2 = Product.builder()
                    .name("뚝섬 윈드서핑 역동의 중류수")
                    .price(1500)
                    .stock(100)
                    .description("서울 시민들의 땀과 열정이 0.001% 함유된 짜릿한 도심 에디션")
                    .productStatus(ProductStatus.ON_SALE)
                    .productCategory(ProductCategory.MIDSTREAM)
                    .build();

            Product p3 = Product.builder()
                    .name("반포대교 달빛무지개분수 직수")
                    .price(3000)
                    .stock(0)
                    .description("무지개 조명을 받아 화려한 색감을 자랑하는 인스타 감성 뿜뿜 한강물 (현재 품절)")
                    .productStatus(ProductStatus.SOLD_OUT) // 품절 상태 테스트용!
                    .productCategory(ProductCategory.MIDSTREAM)
                    .build();

            Product p4 = Product.builder()
                    .name("김포 하류 노을빛 감성수")
                    .price(2000)
                    .stock(50)
                    .description("서해로 빠져나가기 직전, 석양을 머금어 묘한 짠맛이 나는 한강물")
                    .productStatus(ProductStatus.ON_SALE)
                    .productCategory(ProductCategory.DOWNSTREAM)
                    .build();

            Product p5 = Product.builder()
                    .name("여의도 봄꽃축제 벚꽃 에디션")
                    .price(2500)
                    .stock(200)
                    .description("흩날리는 벚꽃잎이 우연히 들어간 달콤쌉싸름한 봄 한정판 한강물")
                    .productStatus(ProductStatus.ON_SALE)
                    .productCategory(ProductCategory.DOWNSTREAM)
                    .build();



            // 앱 실행하면 데이터 밀어넣기 (더미데이터)
            productRepository.saveAll(List.of(p1, p2, p3, p4, p5));
        }
    }
}
