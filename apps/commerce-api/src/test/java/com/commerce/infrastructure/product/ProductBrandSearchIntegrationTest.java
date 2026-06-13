package com.commerce.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
}
