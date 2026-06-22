package com.commerce.application.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class PurchasableItemResolverTest {

    private static final Long SKU_ID = 10L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long BRAND_ID = 2L;

    @Mock
    private SkuRepository skuRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BrandRepository brandRepository;

    private PurchasableItemResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PurchasableItemResolver(skuRepository, productRepository, brandRepository);
    }

    private Sku sku(int stock) {
        return Sku.reconstitute(SKU_ID, PRODUCT_ID, List.of(new OptionValue("색상", "빨강")),
            new Money(10000), new Money(8000), new Stock(stock));
    }

    private Product product(ProductStatus status) {
        return Product.reconstitute(PRODUCT_ID, "맨투맨", "설명", 1L, BRAND_ID, "img.jpg", status);
    }

    private Brand brand(BrandStatus status) {
        return Brand.reconstitute(BRAND_ID, "나이키", "logo.jpg", status);
    }

    @Nested
    @DisplayName("구매 가능성 해소(resolve)")
    class Resolve {

        @Test
        @DisplayName("정상 카탈로그면 SKU·상품·브랜드를 담은 PurchasableItem을 반환한다")
        void should_returnItem_when_validCatalog() {
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(100)));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
            given(brandRepository.findById(BRAND_ID)).willReturn(Optional.of(brand(BrandStatus.ACTIVE)));

            PurchasableItem item = resolver.resolve(SKU_ID);

            assertThat(item.sku().getId()).isEqualTo(SKU_ID);
            assertThat(item.product().getId()).isEqualTo(PRODUCT_ID);
            assertThat(item.brand().getId()).isEqualTo(BRAND_ID);
        }

        @Test
        @DisplayName("존재하지 않는 SKU면 NOT_FOUND 예외가 발생한다")
        void should_throwNotFound_when_skuMissing() {
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> resolver.resolve(SKU_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 상품이면 NOT_FOUND 예외가 발생한다")
        void should_throwNotFound_when_productMissing() {
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(100)));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> resolver.resolve(SKU_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("판매 중이 아닌 상품이면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_productNotVisible() {
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(100)));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.SUSPENDED)));

            assertThatThrownBy(() -> resolver.resolve(SKU_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("존재하지 않는 브랜드면 NOT_FOUND 예외가 발생한다")
        void should_throwNotFound_when_brandMissing() {
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(100)));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
            given(brandRepository.findById(BRAND_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> resolver.resolve(SKU_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("비활성 브랜드면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_brandInactive() {
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(100)));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
            given(brandRepository.findById(BRAND_ID)).willReturn(Optional.of(brand(BrandStatus.INACTIVE)));

            assertThatThrownBy(() -> resolver.resolve(SKU_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("재고 검증(requireEnoughStock)")
    class RequireEnoughStock {

        @Test
        @DisplayName("재고가 부족하면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_insufficientStock() {
            PurchasableItem item = new PurchasableItem(
                sku(2), product(ProductStatus.ON_SALE), brand(BrandStatus.ACTIVE));

            assertThatThrownBy(() -> resolver.requireEnoughStock(item, 3))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("재고가 충분하면 통과한다")
        void should_pass_when_enoughStock() {
            PurchasableItem item = new PurchasableItem(
                sku(10), product(ProductStatus.ON_SALE), brand(BrandStatus.ACTIVE));

            assertThatCode(() -> resolver.requireEnoughStock(item, 10)).doesNotThrowAnyException();
        }
    }
}
