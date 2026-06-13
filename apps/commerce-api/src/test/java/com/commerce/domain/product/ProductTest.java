package com.commerce.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class ProductTest {

    private static final String VALID_NAME = "맨투맨";
    private static final String VALID_DESCRIPTION = "기모 맨투맨";
    private static final Long VALID_CATEGORY_ID = 1L;
    private static final Long VALID_BRAND_ID = 2L;
    private static final String VALID_IMAGE_URL = "https://img.example.com/1.jpg";

    private static Product registered() {
        return Product.register(VALID_NAME, VALID_DESCRIPTION, VALID_CATEGORY_ID, VALID_BRAND_ID, VALID_IMAGE_URL);
    }

    @Nested
    @DisplayName("register (신규 등록)")
    class Register {

        @Test
        @DisplayName("id 없이 ON_SALE 상태로 생성된다")
        void should_createWithOnSale_andNoId_when_register() {
            // when
            Product product = registered();

            // then
            assertThat(product)
                .satisfies(p -> assertThat(p.getId()).isNull())
                .satisfies(p -> assertThat(p.getName()).isEqualTo(VALID_NAME))
                .satisfies(p -> assertThat(p.getCategoryId()).isEqualTo(VALID_CATEGORY_ID))
                .satisfies(p -> assertThat(p.getStatus()).isEqualTo(ProductStatus.ON_SALE));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("상품명이 공백이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_nameBlank(String name) {
            // when & then
            assertThatThrownBy(() -> Product.register(name, VALID_DESCRIPTION, VALID_CATEGORY_ID, VALID_BRAND_ID, VALID_IMAGE_URL))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("상품명이 100자를 초과하면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_nameTooLong() {
            // given
            String tooLong = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> Product.register(tooLong, VALID_DESCRIPTION, VALID_CATEGORY_ID, VALID_BRAND_ID, VALID_IMAGE_URL))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("카테고리 ID가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_categoryIdNull() {
            // when & then
            assertThatThrownBy(() -> Product.register(VALID_NAME, VALID_DESCRIPTION, null, VALID_BRAND_ID, VALID_IMAGE_URL))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("상태 전이")
    class StatusTransition {

        @Test
        @DisplayName("판매중이면 일시중지할 수 있다")
        void should_suspend_when_onSale() {
            // given
            Product product = registered();

            // when
            product.suspend();

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
        }

        @Test
        @DisplayName("판매중이 아니면 일시중지할 수 없다")
        void should_throwException_when_suspend_notOnSale() {
            // given
            Product product = registered();
            product.suspend();

            // when & then
            assertThatThrownBy(product::suspend)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("일시중지 상태면 판매를 재개할 수 있다")
        void should_resume_when_suspended() {
            // given
            Product product = registered();
            product.suspend();

            // when
            product.resume();

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        }

        @Test
        @DisplayName("일시중지 상태가 아니면 재개할 수 없다")
        void should_throwException_when_resume_notSuspended() {
            // given
            Product onSale = registered();
            Product discontinued = registered();
            discontinued.discontinue();

            // when & then
            assertThatThrownBy(onSale::resume)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            assertThatThrownBy(discontinued::resume)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("판매중·일시중지 상태에서 단종할 수 있다")
        void should_discontinue_when_onSaleOrSuspended() {
            // given
            Product fromOnSale = registered();
            Product fromSuspended = registered();
            fromSuspended.suspend();

            // when
            fromOnSale.discontinue();
            fromSuspended.discontinue();

            // then
            assertThat(fromOnSale.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
            assertThat(fromSuspended.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
        }

        @Test
        @DisplayName("이미 단종된 상품은 다시 단종할 수 없다")
        void should_throwException_when_discontinue_alreadyDiscontinued() {
            // given
            Product product = registered();
            product.discontinue();

            // when & then
            assertThatThrownBy(product::discontinue)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("isVisible")
    class IsVisible {

        @Test
        @DisplayName("ON_SALE이면 true, 그 외 상태면 false다")
        void should_returnVisibility_when_isVisible() {
            // given
            Product onSale = registered();
            Product suspended = registered();
            suspended.suspend();
            Product discontinued = registered();
            discontinued.discontinue();

            // when & then
            assertThat(onSale.isVisible()).isTrue();
            assertThat(suspended.isVisible()).isFalse();
            assertThat(discontinued.isVisible()).isFalse();
        }
    }
}
