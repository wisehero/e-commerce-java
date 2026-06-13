package com.commerce.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class StockTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 1000})
        @DisplayName("0 이상이면 정상 생성된다")
        void should_create_when_zeroOrPositive(int quantity) {
            // when
            Stock stock = new Stock(quantity);

            // then
            assertThat(stock.quantity()).isEqualTo(quantity);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, Integer.MIN_VALUE})
        @DisplayName("음수면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_negative(int quantity) {
            // when & then
            assertThatThrownBy(() -> new Stock(quantity))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("decrease (차감)")
    class Decrease {

        @Test
        @DisplayName("재고가 충분하면 차감된 새 Stock을 반환하고 원본은 불변이다")
        void should_returnDecreasedStock_when_enough() {
            // given
            Stock stock = new Stock(10);

            // when
            Stock decreased = stock.decrease(3);

            // then
            assertThat(decreased.quantity()).isEqualTo(7);
            assertThat(stock.quantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("재고보다 많이 차감하면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_insufficient() {
            // given
            Stock stock = new Stock(5);

            // when & then
            assertThatThrownBy(() -> stock.decrease(6))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("차감 수량이 1 미만이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_decreaseCountZeroOrNegative(int count) {
            // given
            Stock stock = new Stock(10);

            // when & then
            assertThatThrownBy(() -> stock.decrease(count))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("increase (증가)")
    class Increase {

        @Test
        @DisplayName("증가된 새 Stock을 반환하고 원본은 불변이다")
        void should_returnIncreasedStock_when_increase() {
            // given
            Stock stock = new Stock(10);

            // when
            Stock increased = stock.increase(5);

            // then
            assertThat(increased.quantity()).isEqualTo(15);
            assertThat(stock.quantity()).isEqualTo(10);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("증가 수량이 1 미만이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_increaseCountZeroOrNegative(int count) {
            // given
            Stock stock = new Stock(10);

            // when & then
            assertThatThrownBy(() -> stock.increase(count))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
