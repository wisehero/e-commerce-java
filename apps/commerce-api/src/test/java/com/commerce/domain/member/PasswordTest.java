package com.commerce.domain.member;

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

class PasswordTest {

    private final PasswordHasher hasher = new FakePasswordHasher();

    @Nested
    @DisplayName("of (신규 생성)")
    class Of {

        @Test
        @DisplayName("평문을 해셔로 해싱해 저장한다")
        void should_hashRawPassword_when_of() {
            // given
            String raw = "password123";

            // when
            Password password = Password.of(raw, hasher);

            // then
            assertThat(password.hashedValue()).isEqualTo("hashed:" + raw);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "a", "1234567"})
        @DisplayName("8자 미만이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_rawPasswordTooShort(String raw) {
            // when & then
            assertThatThrownBy(() -> Password.of(raw, hasher))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_rawPasswordNull(String raw) {
            // when & then
            assertThatThrownBy(() -> Password.of(raw, hasher))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("ofHashed (영속 복원)")
    class OfHashed {

        @Test
        @DisplayName("해시값으로 그대로 복원한다")
        void should_create_when_ofHashed() {
            // given
            String hashed = "already-hashed-value";

            // when
            Password password = Password.ofHashed(hashed);

            // then
            assertThat(password.hashedValue()).isEqualTo(hashed);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("null이거나 공백이면 INTERNAL_ERROR 예외가 발생한다")
        void should_throwException_when_hashedValueNullOrBlank(String invalid) {
            // when & then
            assertThatThrownBy(() -> Password.ofHashed(invalid))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.INTERNAL_ERROR);
        }
    }

    @Nested
    @DisplayName("matches")
    class Matches {

        @Test
        @DisplayName("평문이 해시와 일치하면 true를 반환한다")
        void should_returnTrue_when_matchesCorrect() {
            // given
            Password password = Password.of("password123", hasher);

            // when & then
            assertThat(password.matches("password123", hasher)).isTrue();
        }

        @Test
        @DisplayName("평문이 해시와 다르면 false를 반환한다")
        void should_returnFalse_when_matchesWrong() {
            // given
            Password password = Password.of("password123", hasher);

            // when & then
            assertThat(password.matches("wrong-password", hasher)).isFalse();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("해시값을 노출하지 않고 마스킹된 문자열을 반환한다")
        void should_maskHashedValue_when_toString() {
            // given
            Password password = Password.ofHashed("super-secret-hash");

            // when
            String result = password.toString();

            // then
            assertThat(result).isEqualTo("Password[***]").doesNotContain("super-secret-hash");
        }
    }

    /** 테스트용 결정론적 해셔. "hashed:" 접두사 방식으로 해싱·검증한다. */
    private static class FakePasswordHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hashedPassword) {
            return hashedPassword.equals("hashed:" + rawPassword);
        }
    }
}
