package com.commerce.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.category.CategoryRepository;
import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.ProductSearchCondition;
import com.commerce.domain.product.ProductStatus;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.product.Stock;
import com.commerce.domain.shared.Money;
import com.commerce.support.page.PageResult;

@ExtendWith(MockitoExtension.class)
class ProductSearchUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductSearchUseCase useCase;

    private Product productWith(Long id) {
        return Product.reconstitute(id, "상품" + id, "설명", 1L, 2L, "img.jpg", ProductStatus.ON_SALE);
    }

    private Sku skuOf(Long id, Long productId, long salePrice) {
        return Sku.reconstitute(id, productId, List.of(new OptionValue("색상", "빨강")),
            new Money(20000), new Money(salePrice), new Stock(100));
    }

    @Nested
    @DisplayName("상품 목록/검색")
    class Search {

        @Test
        @DisplayName("상품별로 최저 판매가를 계산해 요약 목록을 반환한다")
        void should_returnSummariesWithLowestPrice_when_search() {
            // given
            ProductSearchCondition condition = new ProductSearchCondition(null, null, null, 0, 20);
            given(productRepository.search(condition))
                .willReturn(new PageResult<>(List.of(productWith(1L), productWith(2L)), 2, 0, 20));
            given(skuRepository.findByProductIds(List.of(1L, 2L))).willReturn(List.of(
                skuOf(101L, 1L, 9000),
                skuOf(102L, 1L, 10000),
                skuOf(201L, 2L, 5000)
            ));

            // when
            PageResult<ProductSummaryInfo> result = useCase.search(null, null, null, 0, 20);

            // then
            assertThat(result.items())
                .extracting(ProductSummaryInfo::id, ProductSummaryInfo::lowestSalePrice)
                .containsExactly(tuple(1L, 9000L), tuple(2L, 5000L));
        }

        @Test
        @DisplayName("상품이 없으면 빈 결과를 반환한다")
        void should_returnEmptyResult_when_noProducts() {
            // given
            ProductSearchCondition condition = new ProductSearchCondition(null, null, null, 0, 20);
            given(productRepository.search(condition))
                .willReturn(new PageResult<>(List.of(), 0, 0, 20));
            given(skuRepository.findByProductIds(anyList())).willReturn(List.of());

            // when
            PageResult<ProductSummaryInfo> result = useCase.search(null, null, null, 0, 20);

            // then
            assertThat(result.items()).isEmpty();
            assertThat(result.totalCount()).isZero();
        }

        @Test
        @DisplayName("카테고리 필터는 하위 카테고리까지 펼쳐 ProductSearchCondition으로 전달한다")
        void should_expandCategoryAndPassFilter_when_search() {
            // given
            given(categoryRepository.findSelfAndDescendantIds(1L)).willReturn(List.of(1L, 5L, 6L));
            ProductSearchCondition condition = new ProductSearchCondition("맨투맨", List.of(1L, 5L, 6L), 2L, 0, 20);
            given(productRepository.search(condition))
                .willReturn(new PageResult<>(List.of(), 0, 0, 20));
            given(skuRepository.findByProductIds(anyList())).willReturn(List.of());

            // when
            useCase.search("맨투맨", 1L, 2L, 0, 20);

            // then
            assertThat(condition.categoryIds()).containsExactly(1L, 5L, 6L);
            assertThat(condition.brandId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("카테고리 필터가 없으면 카테고리를 펼치지 않고 그대로 검색한다")
        void should_notExpandCategory_when_categoryIdNull() {
            // given
            ProductSearchCondition condition = new ProductSearchCondition(null, null, null, 0, 20);
            given(productRepository.search(condition))
                .willReturn(new PageResult<>(List.of(), 0, 0, 20));
            given(skuRepository.findByProductIds(anyList())).willReturn(List.of());

            // when
            useCase.search(null, null, null, 0, 20);

            // then
            assertThat(condition.categoryIds()).isNull();
        }
    }
}
