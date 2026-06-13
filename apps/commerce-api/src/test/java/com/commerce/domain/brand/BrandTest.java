package com.commerce.domain.brand;

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

class BrandTest {

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("id 없이 ACTIVE 상태로 생성된다")
        void should_createActiveBrand_when_register() {
            // when
            Brand brand = Brand.register("나이키", "logo.jpg");

            // then
            assertThat(brand)
                .satisfies(b -> assertThat(b.getId()).isNull())
                .satisfies(b -> assertThat(b.getName()).isEqualTo("나이키"))
                .satisfies(b -> assertThat(b.getLogoUrl()).isEqualTo("logo.jpg"))
                .satisfies(b -> assertThat(b.getStatus()).isEqualTo(BrandStatus.ACTIVE));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("브랜드명이 공백이면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_nameBlank(String name) {
            // when & then
            assertThatThrownBy(() -> Brand.register(name, "logo.jpg"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("브랜드명이 50자를 초과하면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_nameTooLong() {
            // when & then
            assertThatThrownBy(() -> Brand.register("가".repeat(51), "logo.jpg"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("상태 전이")
    class StatusTransition {

        @Test
        @DisplayName("ACTIVE 브랜드는 비활성화할 수 있다")
        void should_deactivate_when_active() {
            // given
            Brand brand = Brand.register("나이키", "logo.jpg");

            // when
            brand.deactivate();

            // then
            assertThat(brand.getStatus()).isEqualTo(BrandStatus.INACTIVE);
            assertThat(brand.isVisible()).isFalse();
        }

        @Test
        @DisplayName("INACTIVE 브랜드는 활성화할 수 있다")
        void should_activate_when_inactive() {
            // given
            Brand brand = Brand.register("나이키", "logo.jpg");
            brand.deactivate();

            // when
            brand.activate();

            // then
            assertThat(brand.getStatus()).isEqualTo(BrandStatus.ACTIVE);
            assertThat(brand.isVisible()).isTrue();
        }

        @Test
        @DisplayName("이미 ACTIVE면 다시 활성화할 수 없다")
        void should_throwBadRequest_when_activateAlreadyActive() {
            // given
            Brand brand = Brand.register("나이키", "logo.jpg");

            // when & then
            assertThatThrownBy(brand::activate)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("이미 INACTIVE면 다시 비활성화할 수 없다")
        void should_throwBadRequest_when_deactivateAlreadyInactive() {
            // given
            Brand brand = Brand.register("나이키", "logo.jpg");
            brand.deactivate();

            // when & then
            assertThatThrownBy(brand::deactivate)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
