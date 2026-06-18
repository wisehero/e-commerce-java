package com.commerce.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.ProductSearchCondition;
import com.commerce.infrastructure.brand.BrandJpaRepository;
import com.commerce.support.IntegrationTestSupport;
import com.commerce.support.page.PageResult;

class ProductBrandSearchIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        productJpaRepository.deleteAll();
        brandJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("상품 검색은 ACTIVE 브랜드 상품만 노출한다")
    void should_searchOnlyActiveBrandProducts() {
        // given
        Brand activeBrand = brandRepository.save(Brand.register("활성 브랜드", "active.jpg"));
        Brand inactiveBrand = brandRepository.save(Brand.register("비활성 브랜드", "inactive.jpg"));
        txTemplate.executeWithoutResult(s -> {
            Brand brand = brandRepository.findById(inactiveBrand.getId()).orElseThrow();
            brand.deactivate();
            brandRepository.save(brand);
        });

        productRepository.save(Product.register("노출 상품", "설명", 1L, activeBrand.getId(), "p1.jpg"));
        productRepository.save(Product.register("숨김 상품", "설명", 1L, inactiveBrand.getId(), "p2.jpg"));

        // when
        PageResult<Product> result = productRepository.search(new ProductSearchCondition(null, null, null, 0, 10));

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.items())
            .extracting(Product::getName, Product::getBrandId)
            .containsExactly(tuple("노출 상품", activeBrand.getId()));
    }

    @Test
    @DisplayName("brandId 필터에도 ACTIVE 브랜드 게이트가 동일하게 적용된다")
    void should_applyBrandFilterWithActiveGate() {
        // given
        Brand activeBrand = brandRepository.save(Brand.register("활성 브랜드", "active.jpg"));
        Brand inactiveBrand = brandRepository.save(Brand.register("비활성 브랜드", "inactive.jpg"));
        txTemplate.executeWithoutResult(s -> {
            Brand brand = brandRepository.findById(inactiveBrand.getId()).orElseThrow();
            brand.deactivate();
            brandRepository.save(brand);
        });

        productRepository.save(Product.register("노출 상품", "설명", 1L, activeBrand.getId(), "p1.jpg"));
        productRepository.save(Product.register("숨김 상품", "설명", 1L, inactiveBrand.getId(), "p2.jpg"));

        // when
        PageResult<Product> activeResult =
            productRepository.search(new ProductSearchCondition(null, null, activeBrand.getId(), 0, 10));
        PageResult<Product> inactiveResult =
            productRepository.search(new ProductSearchCondition(null, null, inactiveBrand.getId(), 0, 10));

        // then
        assertThat(activeResult.items()).hasSize(1);
        assertThat(inactiveResult.items()).isEmpty();
    }

    @Test
    @DisplayName("categoryIds 목록으로 검색하면 해당 카테고리 상품만 IN 필터로 노출한다")
    void should_filterByCategoryIds() {
        // given
        Brand brand = brandRepository.save(Brand.register("브랜드", "b.jpg"));
        productRepository.save(Product.register("상의 상품", "설명", 10L, brand.getId(), "p1.jpg"));
        productRepository.save(Product.register("하의 상품", "설명", 20L, brand.getId(), "p2.jpg"));
        productRepository.save(Product.register("신발 상품", "설명", 30L, brand.getId(), "p3.jpg"));

        // when : 상위 카테고리를 펼친 결과(10, 20)를 흉내 낸 id 목록
        PageResult<Product> result =
            productRepository.search(new ProductSearchCondition(null, List.of(10L, 20L), null, 0, 10));

        // then
        assertThat(result.items())
            .extracting(Product::getName)
            .containsExactlyInAnyOrder("상의 상품", "하의 상품");
    }

    @Test
    @DisplayName("categoryIds가 null이면 카테고리 필터 없이 전체를 노출한다")
    void should_notFilterByCategory_when_categoryIdsNull() {
        // given
        Brand brand = brandRepository.save(Brand.register("브랜드", "b.jpg"));
        productRepository.save(Product.register("상품A", "설명", 10L, brand.getId(), "p1.jpg"));
        productRepository.save(Product.register("상품B", "설명", 20L, brand.getId(), "p2.jpg"));

        // when
        PageResult<Product> result =
            productRepository.search(new ProductSearchCondition(null, null, null, 0, 10));

        // then
        assertThat(result.items()).hasSize(2);
    }
}
