package com.commerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class OrderLineTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SKU_ID = 10L;
    private static final String PRODUCT_NAME = "나이키 에어맥스";
    private static final String OPTION_SUMMARY = "색상:블랙 / 사이즈:270";
    private static final Money UNIT_PRICE = new Money(100000);

    private static OrderLine created(int quantity) {
        return OrderLine.create(PRODUCT_ID, SKU_ID, PRODUCT_NAME, OPTION_SUMMARY, UNIT_PRICE, quantity);
    }

    @Nested
    @DisplayName("create (신규 생성)")
    class Create {

        @Test
        @DisplayName("id 없이 스냅샷 값으로 생성된다")
        void should_createWithSnapshot_andNoId_when_create() {
            // when
            OrderLine line = created(2);

            // then
            assertThat(line)
                .satisfies(l -> assertThat(l.getId()).isNull())
                .satisfies(l -> assertThat(l.getProductId()).isEqualTo(PRODUCT_ID))
                .satisfies(l -> assertThat(l.getSkuId()).isEqualTo(SKU_ID))
                .satisfies(l -> assertThat(l.getProductName()).isEqualTo(PRODUCT_NAME))
                .satisfies(l -> assertThat(l.getOptionSummary()).isEqualTo(OPTION_SUMMARY))
                .satisfies(l -> assertThat(l.getUnitPrice()).isEqualTo(UNIT_PRICE))
                .satisfies(l -> assertThat(l.getQuantity()).isEqualTo(2));
        }

        @Test
        @DisplayName("상품 ID가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_productIdNull() {
            // when & then
            assertThatThrownBy(() ->
                OrderLine.create(null, SKU_ID, PRODUCT_NAME, OPTION_SUMMARY, UNIT_PRICE, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("SKU ID가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_skuIdNull() {
            // when & then
            assertThatThrownBy(() ->
                OrderLine.create(PRODUCT_ID, null, PRODUCT_NAME, OPTION_SUMMARY, UNIT_PRICE, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("상품명이 비어 있으면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_productNameBlank() {
            // when & then
            assertThatThrownBy(() ->
                OrderLine.create(PRODUCT_ID, SKU_ID, " ", OPTION_SUMMARY, UNIT_PRICE, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("옵션 정보가 비어 있으면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_optionSummaryBlank() {
            // when & then
            assertThatThrownBy(() ->
                OrderLine.create(PRODUCT_ID, SKU_ID, PRODUCT_NAME, " ", UNIT_PRICE, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("단가가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_unitPriceNull() {
            // when & then
            assertThatThrownBy(() ->
                OrderLine.create(PRODUCT_ID, SKU_ID, PRODUCT_NAME, OPTION_SUMMARY, null, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("수량이 1개 미만이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_quantityBelowOne(int quantity) {
            // when & then
            assertThatThrownBy(() ->
                OrderLine.create(PRODUCT_ID, SKU_ID, PRODUCT_NAME, OPTION_SUMMARY, UNIT_PRICE, quantity))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("reconstitute (영속 복원)")
    class Reconstitute {

        @Test
        @DisplayName("id를 가진 채로 복원된다")
        void should_restoreWithId_when_reconstitute() {
            // when
            OrderLine line = OrderLine.reconstitute(
                99L, PRODUCT_ID, SKU_ID, PRODUCT_NAME, OPTION_SUMMARY, UNIT_PRICE, 3);

            // then
            assertThat(line.getId()).isEqualTo(99L);
            assertThat(line.getQuantity()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("lineAmount")
    class LineAmount {

        @Test
        @DisplayName("라인 금액은 단가 × 수량이다")
        void should_returnUnitPriceTimesQuantity_when_lineAmount() {
            // given
            OrderLine line = created(3);

            // when & then
            assertThat(line.lineAmount()).isEqualTo(new Money(300000));
        }
    }
}
