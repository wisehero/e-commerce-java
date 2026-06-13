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

class OptionValueTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("name과 value가 유효하면 정상 생성된다")
        void should_create_when_valid() {
            // when
            OptionValue optionValue = new OptionValue("색상", "빨강");

            // then
            assertThat(optionValue)
                .satisfies(ov -> assertThat(ov.name()).isEqualTo("색상"))
                .satisfies(ov -> assertThat(ov.value()).isEqualTo("빨강"));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("name이 공백이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_nameBlank(String name) {
            // when & then
            assertThatThrownBy(() -> new OptionValue(name, "빨강"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("value가 공백이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_valueBlank(String value) {
            // when & then
            assertThatThrownBy(() -> new OptionValue("색상", value))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
