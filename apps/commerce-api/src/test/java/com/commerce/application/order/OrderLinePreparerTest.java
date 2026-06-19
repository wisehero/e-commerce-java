package com.commerce.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
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
import com.commerce.domain.product.StockDeducter;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class OrderLinePreparerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SKU_ID = 10L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long BRAND_ID = 2L;

    @Mock
    private SkuRepository skuRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private StockDeducter stockDeducter;

    private OrderLinePreparer preparer;

    @BeforeEach
    void setUp() {
        preparer = new OrderLinePreparer(skuRepository, productRepository, brandRepository,
            Map.of("optimistic", stockDeducter));
    }

    private OrderPlaceCommand command() {
        return new OrderPlaceCommand(MEMBER_ID, List.of(new OrderPlaceCommand.LineCommand(SKU_ID, 2)), "optimistic");
    }

    private Sku sku() {
        return Sku.reconstitute(SKU_ID, PRODUCT_ID, List.of(new OptionValue("색상", "빨강")),
            new Money(10000), new Money(8000), new Stock(100));
    }

    private Product product(ProductStatus status) {
        return Product.reconstitute(PRODUCT_ID, "맨투맨", "설명", 1L, BRAND_ID, "img.jpg", status);
    }

    private Brand brand(BrandStatus status) {
        return Brand.reconstitute(BRAND_ID, "나이키", "logo.jpg", status);
    }

    @Nested
    @DisplayName("전략 선택")
    class ResolveStrategy {

        @Test
        @DisplayName("지원하지 않는 lockMode면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_unknownLockMode() {
            assertThatThrownBy(() -> preparer.resolveStrategy("unknown"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("라인 준비")
    class Prepare {

        @Test
        @DisplayName("정상 카탈로그면 재고를 차감하고 OrderLine·DiscountableLine을 만든다")
        void should_deductAndPrepare_when_validCatalog() {
            // given
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku()));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
            given(brandRepository.findById(BRAND_ID)).willReturn(Optional.of(brand(BrandStatus.ACTIVE)));

            // when
            List<PreparedLine> prepared = preparer.prepare(command(), stockDeducter);

            // then
            assertThat(prepared).hasSize(1);
            PreparedLine line = prepared.get(0);
            assertThat(line.orderLine().lineAmount()).isEqualTo(new Money(16000)); // 판매가 8000 × 2
            assertThat(line.discountableLine().productId()).isEqualTo(PRODUCT_ID);
            assertThat(line.discountableLine().brandId()).isEqualTo(BRAND_ID);
            then(stockDeducter).should().deduct(SKU_ID, 2);
        }

        @Test
        @DisplayName("ON_SALE가 아닌 상품이면 BAD_REQUEST가 발생하고 재고를 차감하지 않는다")
        void should_throwBadRequest_when_productNotVisible() {
            // given
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku()));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.SUSPENDED)));

            // when & then
            assertThatThrownBy(() -> preparer.prepare(command(), stockDeducter))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(stockDeducter).should(never()).deduct(any(), anyInt());
        }

        @Test
        @DisplayName("비활성 브랜드면 BAD_REQUEST가 발생하고 재고를 차감하지 않는다")
        void should_throwBadRequest_when_brandInactive() {
            // given
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku()));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
            given(brandRepository.findById(BRAND_ID)).willReturn(Optional.of(brand(BrandStatus.INACTIVE)));

            // when & then
            assertThatThrownBy(() -> preparer.prepare(command(), stockDeducter))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(stockDeducter).should(never()).deduct(any(), anyInt());
        }

        @Test
        @DisplayName("존재하지 않는 SKU면 NOT_FOUND가 발생한다")
        void should_throwNotFound_when_skuMissing() {
            // given
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> preparer.prepare(command(), stockDeducter))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
