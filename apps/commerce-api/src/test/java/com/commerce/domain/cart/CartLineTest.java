package com.commerce.domain.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class CartLineTest {

    @Test
    @DisplayName("유효한 값으로 생성된다")
    void should_create_when_valid() {
        // when
        CartLine line = CartLine.create(10L, 2);

        // then
        assertThat(line)
            .satisfies(l -> assertThat(l.getId()).isNull())
            .satisfies(l -> assertThat(l.getSkuId()).isEqualTo(10L))
            .satisfies(l -> assertThat(l.getQuantity()).isEqualTo(2));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("수량이 1 미만이면 BAD_REQUEST 예외가 발생한다")
    void should_throwBadRequest_when_quantityBelow1(int quantity) {
        // when & then
        assertThatThrownBy(() -> CartLine.create(10L, quantity))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    @DisplayName("skuId가 null이면 BAD_REQUEST 예외가 발생한다")
    void should_throwBadRequest_when_skuIdNull() {
        // when & then
        assertThatThrownBy(() -> CartLine.create(null, 1))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    @DisplayName("reconstitute는 저장된 id를 복원한다")
    void should_restoreId_when_reconstitute() {
        // when
        CartLine line = CartLine.reconstitute(7L, 10L, 3);

        // then
        assertThat(line.getId()).isEqualTo(7L);
    }
}
