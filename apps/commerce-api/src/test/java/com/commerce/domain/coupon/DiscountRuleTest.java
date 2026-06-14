package com.commerce.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class DiscountRuleTest {

    @Nested
    @DisplayName("calculateDiscount")
    class CalculateDiscount {

        @Test
        @DisplayName("정액 할인은 주문 금액을 넘으면 주문 금액으로 클램프한다")
        void should_clampToOrderAmount_when_fixedDiscountGreaterThanOrderAmount() {
            // given
            DiscountRule rule = new DiscountRule(DiscountType.FIXED, 10000L, null, Money.ZERO);

            // when
            Money discount = rule.calculateDiscount(new Money(7000L));

            // then
            assertThat(discount).isEqualTo(new Money(7000L));
        }

        @Test
        @DisplayName("정률 할인은 정수 나눗셈으로 버림 계산하고 cap을 적용한다")
        void should_floorAndCap_when_rateDiscount() {
            // given
            DiscountRule rule = new DiscountRule(DiscountType.RATE, 15L, new Money(1000L), Money.ZERO);

            // when
            Money discount = rule.calculateDiscount(new Money(9999L));

            // then
            assertThat(discount).isEqualTo(new Money(1000L));
        }

        @Test
        @DisplayName("최소 주문 금액 미달이면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_orderAmountLessThanMinimum() {
            // given
            DiscountRule rule = new DiscountRule(DiscountType.FIXED, 1000L, null, new Money(10000L));

            // when & then
            assertThatThrownBy(() -> rule.calculateDiscount(new Money(9999L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("정률 할인율은 1~100 사이여야 한다")
        void should_throwBadRequest_when_rateOutOfRange() {
            assertThatThrownBy(() -> new DiscountRule(DiscountType.RATE, 101L, null, Money.ZERO))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
