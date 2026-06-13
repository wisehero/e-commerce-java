package com.commerce.domain.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class CartTest {

    private static final Long MEMBER_ID = 1L;

    private static Cart emptyCart() {
        return Cart.create(MEMBER_ID);
    }

    @Nested
    @DisplayName("create (신규 장바구니)")
    class Create {

        @Test
        @DisplayName("빈 장바구니로 id 없이 생성된다")
        void should_createEmpty_when_create() {
            // when
            Cart cart = Cart.create(MEMBER_ID);

            // then
            assertThat(cart)
                .satisfies(c -> assertThat(c.getId()).isNull())
                .satisfies(c -> assertThat(c.getMemberId()).isEqualTo(MEMBER_ID))
                .satisfies(c -> assertThat(c.isEmpty()).isTrue());
        }

        @Test
        @DisplayName("소유자가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_memberIdNull() {
            // when & then
            assertThatThrownBy(() -> Cart.create(null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("addItem (담기)")
    class AddItem {

        @Test
        @DisplayName("새 SKU는 새 라인으로 추가된다")
        void should_addNewLine_when_newSku() {
            // given
            Cart cart = emptyCart();

            // when
            cart.addItem(10L, 2);

            // then
            assertThat(cart.getLines())
                .singleElement()
                .satisfies(line -> assertThat(line.getSkuId()).isEqualTo(10L))
                .satisfies(line -> assertThat(line.getQuantity()).isEqualTo(2));
        }

        @Test
        @DisplayName("같은 SKU를 다시 담으면 수량이 합산된다")
        void should_mergeQuantity_when_sameSku() {
            // given
            Cart cart = emptyCart();

            // when
            cart.addItem(10L, 2);
            cart.addItem(10L, 3);

            // then
            assertThat(cart.getLines()).hasSize(1);
            assertThat(cart.quantityOf(10L)).isEqualTo(5);
        }

        @Test
        @DisplayName("담을 수 있는 상품 종류 상한을 넘으면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_exceedMaxDistinctLines() {
            // given
            Cart cart = emptyCart();
            for (long skuId = 1; skuId <= Cart.MAX_DISTINCT_LINES; skuId++) {
                cart.addItem(skuId, 1);
            }

            // when & then
            assertThatThrownBy(() -> cart.addItem(9_999L, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("changeQuantity (수량 변경)")
    class ChangeQuantity {

        @Test
        @DisplayName("기존 라인의 수량을 절대값으로 변경한다")
        void should_changeToAbsolute_when_lineExists() {
            // given
            Cart cart = emptyCart();
            cart.addItem(10L, 2);

            // when
            cart.changeQuantity(10L, 5);

            // then
            assertThat(cart.quantityOf(10L)).isEqualTo(5);
        }

        @Test
        @DisplayName("장바구니에 없는 SKU면 NOT_FOUND 예외가 발생한다")
        void should_throwNotFound_when_lineAbsent() {
            // given
            Cart cart = emptyCart();

            // when & then
            assertThatThrownBy(() -> cart.changeQuantity(99L, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("removeItem (라인 제거)")
    class RemoveItem {

        @Test
        @DisplayName("담긴 SKU 라인을 제거한다")
        void should_removeLine_when_present() {
            // given
            Cart cart = emptyCart();
            cart.addItem(10L, 2);
            cart.addItem(11L, 1);

            // when
            cart.removeItem(10L);

            // then
            assertThat(cart.getLines())
                .singleElement()
                .satisfies(line -> assertThat(line.getSkuId()).isEqualTo(11L));
        }

        @Test
        @DisplayName("없는 SKU 제거는 멱등하게 아무 일도 하지 않는다")
        void should_doNothing_when_absent() {
            // given
            Cart cart = emptyCart();
            cart.addItem(10L, 2);

            // when
            cart.removeItem(99L);

            // then
            assertThat(cart.getLines()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("clear (비우기)")
    class Clear {

        @Test
        @DisplayName("모든 라인을 비운다")
        void should_emptyAllLines_when_clear() {
            // given
            Cart cart = emptyCart();
            cart.addItem(10L, 2);
            cart.addItem(11L, 1);

            // when
            cart.clear();

            // then
            assertThat(cart.isEmpty()).isTrue();
        }
    }
}
