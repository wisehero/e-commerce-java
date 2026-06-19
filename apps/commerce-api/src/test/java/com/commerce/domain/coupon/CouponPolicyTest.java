package com.commerce.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.commerce.domain.member.MemberGrade;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class CouponPolicyTest {

    private static final ZonedDateTime NOW = ZonedDateTime.parse("2026-06-14T10:00:00+09:00[Asia/Seoul]");
    private static final DiscountRule BASE_RULE = new DiscountRule(DiscountType.FIXED, 1000L, null, Money.ZERO);

    private CouponPolicy policy(boolean active, long issuedCount, long maxIssueCount) {
        return CouponPolicy.reconstitute(1L, "선착순 쿠폰",
            BASE_RULE, ApplicabilityScope.whole(), Map.of(),
            7, NOW.minusDays(1), NOW.plusDays(1), maxIssueCount, issuedCount, active);
    }

    @Nested
    @DisplayName("assertIssuable")
    class AssertIssuable {

        @Test
        @DisplayName("활성 상태이고 기간 안이며 수량이 남아 있으면 예외가 발생하지 않는다")
        void should_notThrow_when_issuable() {
            policy(true, 0L, 10L).assertIssuable(NOW);
        }

        @Test
        @DisplayName("비활성 정책이면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_inactive() {
            assertThatThrownBy(() -> policy(false, 0L, 10L).assertIssuable(NOW))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("발급 수량이 소진되면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_soldOut() {
            assertThatThrownBy(() -> policy(true, 10L, 10L).assertIssuable(NOW))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("resolveRuleFor (등급별 차등 할인 해소)")
    class ResolveRuleFor {

        private final DiscountRule goldRule = new DiscountRule(DiscountType.RATE, 20L, null, Money.ZERO);

        private CouponPolicy gradeDifferentiatedPolicy() {
            return CouponPolicy.reconstitute(1L, "등급 차등 쿠폰",
                BASE_RULE, ApplicabilityScope.whole(), Map.of(MemberGrade.GOLD, goldRule),
                7, NOW.minusDays(1), NOW.plusDays(1), 10L, 0L, true);
        }

        @Test
        @DisplayName("override가 있는 등급은 그 규칙을 돌려준다")
        void should_returnOverride_when_gradeHasOverride() {
            assertThat(gradeDifferentiatedPolicy().resolveRuleFor(MemberGrade.GOLD)).isEqualTo(goldRule);
        }

        @Test
        @DisplayName("override가 없는 등급은 기본 규칙을 돌려준다")
        void should_returnBaseRule_when_gradeHasNoOverride() {
            assertThat(gradeDifferentiatedPolicy().resolveRuleFor(MemberGrade.BRONZE)).isEqualTo(BASE_RULE);
        }
    }
}
