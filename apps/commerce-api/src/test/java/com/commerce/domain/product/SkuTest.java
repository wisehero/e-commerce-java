package com.commerce.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class SkuTest {

    private static final Long PRODUCT_ID = 1L;
    private static final List<OptionValue> OPTIONS = List.of(
        new OptionValue("색상", "빨강"),
        new OptionValue("사이즈", "L")
    );
    private static final Money ORIGINAL = new Money(10000);
    private static final Stock STOCK = new Stock(100);

    private static Sku created() {
        return Sku.create(PRODUCT_ID, OPTIONS, ORIGINAL, STOCK);
    }

    @Nested
    @DisplayName("create (신규 생성)")
    class Create {

        @Test
        @DisplayName("id 없이 판매가=정가로 생성된다")
        void should_createWithSalePriceEqualOriginal_andNoId_when_create() {
            // when
            Sku sku = created();

            // then
            assertThat(sku)
                .satisfies(s -> assertThat(s.getId()).isNull())
                .satisfies(s -> assertThat(s.getProductId()).isEqualTo(PRODUCT_ID))
                .satisfies(s -> assertThat(s.getOriginalPrice()).isEqualTo(ORIGINAL))
                .satisfies(s -> assertThat(s.getSalePrice()).isEqualTo(ORIGINAL))
                .satisfies(s -> assertThat(s.isDiscounted()).isFalse());
        }

        @Test
        @DisplayName("상품 ID가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_productIdNull() {
            // when & then
            assertThatThrownBy(() -> Sku.create(null, OPTIONS, ORIGINAL, STOCK))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("옵션이 비어 있으면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_optionsEmpty() {
            // when & then
            assertThatThrownBy(() -> Sku.create(PRODUCT_ID, List.of(), ORIGINAL, STOCK))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("옵션명이 중복되면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_duplicateOptionName() {
            // given
            List<OptionValue> duplicated = List.of(
                new OptionValue("색상", "빨강"),
                new OptionValue("색상", "파랑")
            );

            // when & then
            assertThatThrownBy(() -> Sku.create(PRODUCT_ID, duplicated, ORIGINAL, STOCK))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("판매가가 정가보다 크면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_salePriceAboveOriginal() {
            // when & then  (create는 판매가=정가라 reconstitute로 위반 상황을 만든다)
            assertThatThrownBy(() -> Sku.reconstitute(
                    1L, PRODUCT_ID, OPTIONS, new Money(10000), new Money(10001), STOCK))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("할인")
    class Discount {

        @Test
        @DisplayName("정가 이하의 할인가를 적용할 수 있다")
        void should_applyDiscount_when_belowOriginal() {
            // given
            Sku sku = created();

            // when
            sku.applyDiscount(new Money(8000));

            // then
            assertThat(sku.getSalePrice()).isEqualTo(new Money(8000));
            assertThat(sku.isDiscounted()).isTrue();
        }

        @Test
        @DisplayName("정가보다 높은 할인가는 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_discountAboveOriginal() {
            // given
            Sku sku = created();

            // when & then
            assertThatThrownBy(() -> sku.applyDiscount(new Money(10001)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("할인을 해제하면 판매가가 정가로 돌아간다")
        void should_resetSaleToOriginal_when_clearDiscount() {
            // given
            Sku sku = created();
            sku.applyDiscount(new Money(8000));

            // when
            sku.clearDiscount();

            // then
            assertThat(sku.getSalePrice()).isEqualTo(ORIGINAL);
            assertThat(sku.isDiscounted()).isFalse();
        }

        @Test
        @DisplayName("isDiscounted는 판매가가 정가보다 낮을 때만 true다")
        void should_returnDiscountedFlag_when_isDiscounted() {
            // given
            Sku notDiscounted = created();
            Sku discounted = created();
            discounted.applyDiscount(new Money(9000));

            // when & then
            assertThat(notDiscounted.isDiscounted()).isFalse();
            assertThat(discounted.isDiscounted()).isTrue();
        }
    }

    @Nested
    @DisplayName("changePrice (정가 변경)")
    class ChangePrice {

        @Test
        @DisplayName("정가를 바꾸면 할인이 초기화되어 판매가도 새 정가를 따른다")
        void should_resetDiscount_when_changePrice() {
            // given
            Sku sku = created();
            sku.applyDiscount(new Money(8000));

            // when
            sku.changePrice(new Money(12000));

            // then
            assertThat(sku.getOriginalPrice()).isEqualTo(new Money(12000));
            assertThat(sku.getSalePrice()).isEqualTo(new Money(12000));
            assertThat(sku.isDiscounted()).isFalse();
        }
    }

    @Nested
    @DisplayName("재고")
    class StockChange {

        @Test
        @DisplayName("restock하면 판매가능재고가 증가한다")
        void should_increaseStock_when_restock() {
            // given
            Sku sku = created();

            // when
            sku.restock(50);

            // then
            assertThat(sku.getStock().quantity()).isEqualTo(150);
        }

        @Test
        @DisplayName("decreaseStock하면 판매가능재고가 감소한다")
        void should_decreaseStock_when_decreaseStock() {
            // given
            Sku sku = created();

            // when
            sku.decreaseStock(30);

            // then
            assertThat(sku.getStock().quantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("재고보다 많이 차감하면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_decreaseStock_insufficient() {
            // given
            Sku sku = created();

            // when & then
            assertThatThrownBy(() -> sku.decreaseStock(101))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
