package com.commerce.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class MoneyTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @ParameterizedTest
        @ValueSource(longs = {0L, 1L, 1000L, Long.MAX_VALUE})
        @DisplayName("0 이상이면 정상 생성된다")
        void should_create_when_zeroOrPositive(long amount) {
            // when
            Money money = new Money(amount);

            // then
            assertThat(money.amount()).isEqualTo(amount);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, Long.MIN_VALUE})
        @DisplayName("음수면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_negative(long amount) {
            // when & then
            assertThatThrownBy(() -> new Money(amount))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("isGreaterThan")
    class IsGreaterThan {

        @Test
        @DisplayName("금액이 더 크면 true다")
        void should_returnTrue_when_amountGreater() {
            // when & then
            assertThat(new Money(1000).isGreaterThan(new Money(999))).isTrue();
        }

        @Test
        @DisplayName("금액이 같거나 작으면 false다")
        void should_returnFalse_when_equalOrLess() {
            // when & then
            assertThat(new Money(1000).isGreaterThan(new Money(1000))).isFalse();
            assertThat(new Money(1000).isGreaterThan(new Money(1001))).isFalse();
        }
    }

    @Nested
    @DisplayName("plus")
    class Plus {

        @Test
        @DisplayName("두 금액을 더한다")
        void should_returnSum_when_plus() {
            // when & then
            assertThat(new Money(1000).plus(new Money(500))).isEqualTo(new Money(1500));
        }

        @Test
        @DisplayName("ZERO에 더하면 그대로다")
        void should_returnSame_when_plusZero() {
            // when & then
            assertThat(Money.ZERO.plus(new Money(1000))).isEqualTo(new Money(1000));
        }
    }

    @Nested
    @DisplayName("multiply")
    class Multiply {

        @Test
        @DisplayName("단가에 수량을 곱한다")
        void should_returnProduct_when_multiply() {
            // when & then
            assertThat(new Money(1500).multiply(3)).isEqualTo(new Money(4500));
        }
    }

    @Nested
    @DisplayName("ZERO")
    class Zero {

        @Test
        @DisplayName("ZERO의 금액은 0이다")
        void should_beZeroAmount_when_ZERO() {
            // when & then
            assertThat(Money.ZERO.amount()).isZero();
        }
    }
}
