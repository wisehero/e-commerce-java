package com.commerce.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.commerce.domain.member.MemberGrade;
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
            ApplicabilityScope.whole(), Map.of(),
            7, NOW.minusDays(1), NOW.plusDays(1), 10L, 0L, true);
    }

    /** WHOLE scope 계산에 쓸 단일 라인. productId·brandId·categoryId는 매칭에 안 쓰여 임의값. */
    private List<DiscountableLine> lines(long amount) {
        return List.of(new DiscountableLine(new Money(amount), 1L, 1L, 1L));
    }

    @Nested
    @DisplayName("issue")
    class Issue {

        @Test
        @DisplayName("발급일 + 유효일수의 자정으로 만료 시각을 계산한다")
        void should_calculateExpiresAtAtStartOfDay_when_issue() {
            // when
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, MemberGrade.BRONZE, NOW);

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
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, MemberGrade.BRONZE, NOW);

            // when
            coupon.use(MEMBER_ID, lines(10000L), Set.of(), NOW.plusMinutes(1), ORDER_ID);

            // then
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
            assertThat(coupon.getUsedOrderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("만료 시각과 같은 순간부터는 사용할 수 없다")
        void should_throwBadRequest_when_nowEqualsExpiresAt() {
            // given
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, MemberGrade.BRONZE, NOW);

            // when & then
            assertThatThrownBy(() -> coupon.use(MEMBER_ID, lines(10000L), Set.of(), coupon.getExpiresAt(), ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("쿠폰 소유자가 다르면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_ownerMismatch() {
            // given
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, MemberGrade.BRONZE, NOW);

            // when & then
            assertThatThrownBy(() -> coupon.use(999L, lines(10000L), Set.of(), NOW.plusMinutes(1), ORDER_ID))
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
            IssuedCoupon coupon = IssuedCoupon.issue(policy(), MEMBER_ID, MemberGrade.BRONZE, NOW);
            coupon.use(MEMBER_ID, lines(10000L), Set.of(), NOW.plusMinutes(1), ORDER_ID);

            // when
            coupon.restore(ORDER_ID);

            // then
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.UNUSED);
            assertThat(coupon.getUsedOrderId()).isNull();
        }
    }

    @Nested
    @DisplayName("calculateDiscount (적용 범위 매칭)")
    class CalculateDiscount {

        private IssuedCoupon scopedCoupon(ApplicabilityScope scope, DiscountRule rule) {
            return IssuedCoupon.reconstitute(1L, 10L, MEMBER_ID, scope, rule,
                CouponStatus.UNUSED, NOW, NOW.plusDays(7), null);
        }

        @Test
        @DisplayName("BRAND scope는 해당 브랜드 라인 부분합에만 할인한다")
        void should_discountOnlyMatchingBrand_when_brandScope() {
            // given: 10% 브랜드(7) 한정 쿠폰, 주문에 브랜드 7(1만)과 브랜드 8(5천)
            IssuedCoupon coupon = scopedCoupon(
                ApplicabilityScope.brand(7L), new DiscountRule(DiscountType.RATE, 10L, null, Money.ZERO));
            List<DiscountableLine> lines = List.of(
                new DiscountableLine(new Money(10000L), 1L, 7L, 100L),
                new DiscountableLine(new Money(5000L), 2L, 8L, 200L));

            // when
            Money discount = coupon.calculateDiscount(lines, Set.of());

            // then: 브랜드 7 부분합 1만의 10% = 1천
            assertThat(discount).isEqualTo(new Money(1000L));
        }

        @Test
        @DisplayName("CATEGORY scope는 해소된 서브트리에 속한 라인만 할인한다")
        void should_discountOnlySubtree_when_categoryScope() {
            // given: 카테고리 1(서브트리 {1,2,3}) 한정 10% 쿠폰
            IssuedCoupon coupon = scopedCoupon(
                ApplicabilityScope.category(1L), new DiscountRule(DiscountType.RATE, 10L, null, Money.ZERO));
            List<DiscountableLine> lines = List.of(
                new DiscountableLine(new Money(10000L), 1L, 7L, 3L),   // 서브트리 안
                new DiscountableLine(new Money(5000L), 2L, 8L, 9L));    // 서브트리 밖

            // when
            Money discount = coupon.calculateDiscount(lines, Set.of(1L, 2L, 3L));

            // then: 카테고리 3 부분합 1만의 10% = 1천
            assertThat(discount).isEqualTo(new Money(1000L));
        }

        @Test
        @DisplayName("매칭되는 라인이 없으면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_noMatchingLine() {
            // given: 브랜드(7) 한정 쿠폰인데 주문엔 브랜드 8만
            IssuedCoupon coupon = scopedCoupon(
                ApplicabilityScope.brand(7L), new DiscountRule(DiscountType.RATE, 10L, null, Money.ZERO));
            List<DiscountableLine> lines = List.of(new DiscountableLine(new Money(5000L), 2L, 8L, 200L));

            // when & then
            assertThatThrownBy(() -> coupon.calculateDiscount(lines, Set.of()))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
