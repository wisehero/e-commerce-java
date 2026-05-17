package com.commerce.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class EmailTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("올바른 형식이면 정상 생성된다")
        void should_create_when_validFormat() {
            // given
            String value = "user@example.com";

            // when
            Email email = new Email(value);

            // then
            assertThat(email.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("값이 같으면 동등하다")
        void should_beEqual_when_sameValue() {
            // given
            Email a = new Email("user@example.com");
            Email b = new Email("user@example.com");

            // when & then
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }

        @Test
        @DisplayName("null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_null() {
            // when & then
            assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("공백 문자열이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_blank() {
            // when & then
            assertThatThrownBy(() -> new Email("   "))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "abc@", "abc@def", "@def.com", "a b@c.com"})
        @DisplayName("형식이 올바르지 않으면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_invalidFormat(String invalid) {
            // when & then
            assertThatThrownBy(() -> new Email(invalid))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
