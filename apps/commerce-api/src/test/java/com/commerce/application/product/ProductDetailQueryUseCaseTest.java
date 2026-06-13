package com.commerce.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.brand.BrandStatus;
import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.ProductStatus;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.product.Stock;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class ProductDetailQueryUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private ProductDetailQueryUseCase useCase;

    private Product productWith(Long id, ProductStatus status) {
        return Product.reconstitute(id, "맨투맨", "기모", 1L, 2L, "img.jpg", status);
    }

    private Sku skuOf(Long id, Long productId) {
        return Sku.reconstitute(id, productId, List.of(new OptionValue("색상", "빨강")),
            new Money(10000), new Money(10000), new Stock(100));
    }

    private Brand brandWith(BrandStatus status) {
        return Brand.reconstitute(2L, "나이키", "logo.jpg", status);
    }

    @Nested
    @DisplayName("상품 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("판매중 상품이면 옵션을 조립해 ProductDetailInfo를 반환한다")
        void should_returnDetailInfo_when_visibleProduct() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.of(productWith(1L, ProductStatus.ON_SALE)));
            given(brandRepository.findById(2L)).willReturn(Optional.of(brandWith(BrandStatus.ACTIVE)));
            given(skuRepository.findByProductId(1L)).willReturn(List.of(skuOf(100L, 1L)));

            // when
            ProductDetailInfo info = useCase.getDetail(1L);

            // then
            assertThat(info)
                .satisfies(i -> assertThat(i.id()).isEqualTo(1L))
                .satisfies(i -> assertThat(i.brandName()).isEqualTo("나이키"))
                .satisfies(i -> assertThat(i.skus()).hasSize(1));
        }

        @Test
        @DisplayName("상품이 없으면 NOT_FOUND 예외가 발생하고 옵션을 조회하지 않는다")
        void should_throwNotFound_when_productMissing() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.getDetail(1L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
            then(skuRepository).should(never()).findByProductId(anyLong());
        }

        @Test
        @DisplayName("브랜드가 비활성이면 NOT_FOUND로 숨기고 옵션을 조회하지 않는다")
        void should_throwNotFound_when_brandNotVisible() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.of(productWith(1L, ProductStatus.ON_SALE)));
            given(brandRepository.findById(2L)).willReturn(Optional.of(brandWith(BrandStatus.INACTIVE)));

            // when & then
            assertThatThrownBy(() -> useCase.getDetail(1L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
            then(skuRepository).should(never()).findByProductId(anyLong());
        }

        @Test
        @DisplayName("판매중이 아닌 상품(일시중지)은 NOT_FOUND로 숨긴다")
        void should_throwNotFound_when_productNotVisible() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.of(productWith(1L, ProductStatus.SUSPENDED)));

            // when & then
            assertThatThrownBy(() -> useCase.getDetail(1L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
            then(skuRepository).should(never()).findByProductId(anyLong());
            then(brandRepository).should(never()).findById(anyLong());
        }
    }
}
