package com.commerce.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class IssuedCouponTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final ZonedDateTime NOW = ZonedDateTime.parse("2026-06-14T15:30:00+09:00[Asia/Seoul]");

    private CouponPolicy policy() {
        return CouponPolicy.reconstitute(10L, "선착순 쿠폰",
            new DiscountRule(DiscountType.FIXED, 1000L, null, Money.ZERO),
            7, NOW.minusDays(1), NOW.plusDays(1), 10L, 0L, true);
    }

    @Nested
    @DisplayName("issue")
    class Issue {

        @Test
        @DisplayName("발급일 + 유효일수의 자정으로 만료 시각을 계산한다")
        void should_calculateExpiresAtAtStartOfDay_when_issue() {
            // when
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, NOW);

            // then
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.UNUSED);
            assertThat(coupon.getExpiresAt()).isEqualTo(
                ZonedDateTime.parse("2026-06-21T00:00:00+09:00[Asia/Seoul]"));
        }
    }

    @Nested
    @DisplayName("use")
    class Use {

        @Test
        @DisplayName("미사용 쿠폰을 사용하면 USED로 전이하고 주문 ID를 기록한다")
        void should_markUsed_when_valid() {
            // given
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, NOW);

            // when
            coupon.use(MEMBER_ID, new Money(10000L), NOW.plusMinutes(1), ORDER_ID);

            // then
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
            assertThat(coupon.getUsedOrderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("만료 시각과 같은 순간부터는 사용할 수 없다")
        void should_throwBadRequest_when_nowEqualsExpiresAt() {
            // given
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, NOW);

            // when & then
            assertThatThrownBy(() -> coupon.use(MEMBER_ID, new Money(10000L), coupon.getExpiresAt(), ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("쿠폰 소유자가 다르면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_ownerMismatch() {
            // given
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, NOW);

            // when & then
            assertThatThrownBy(() -> coupon.use(999L, new Money(10000L), NOW.plusMinutes(1), ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        @DisplayName("사용된 쿠폰을 복원하면 UNUSED로 돌아가고 주문 ID를 제거한다")
        void should_restoreToUnused_when_used() {
            // given
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, NOW);
            coupon.use(MEMBER_ID, new Money(10000L), NOW.plusMinutes(1), ORDER_ID);

            // when
            coupon.restore(ORDER_ID);

            // then
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.UNUSED);
            assertThat(coupon.getUsedOrderId()).isNull();
        }
    }
}
