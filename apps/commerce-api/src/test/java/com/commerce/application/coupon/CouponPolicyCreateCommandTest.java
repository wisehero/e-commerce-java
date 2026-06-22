package com.commerce.application.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.commerce.domain.coupon.DiscountType;
import com.commerce.domain.coupon.ScopeType;
import com.commerce.domain.member.MemberGrade;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@DisplayName("CouponPolicyCreateCommand 팩토리 - raw string의 도메인 enum 해석")
class CouponPolicyCreateCommandTest {

    private static final ZonedDateTime FROM = ZonedDateTime.parse("2026-01-01T00:00:00+09:00");
    private static final ZonedDateTime UNTIL = ZonedDateTime.parse("2026-12-31T23:59:59+09:00");

    private static CouponPolicyCreateCommand command(String discountType, String scopeType) {
        return CouponPolicyCreateCommand.of(
            "신규가입 할인", discountType, 1000L, null, 0L,
            scopeType, null, List.of(),
            7, FROM, UNTIL, 100L, true);
    }

    @Nested
    @DisplayName("정상 변환")
    class Success {

        @Test
        @DisplayName("discountType·scopeType 문자열이 도메인 enum으로 매핑된다")
        void should_mapStringsToEnums() {
            CouponPolicyCreateCommand command = command("FIXED", "BRAND");

            assertThat(command.discountType()).isEqualTo(DiscountType.FIXED);
            assertThat(command.scopeType()).isEqualTo(ScopeType.BRAND);
        }

        @Test
        @DisplayName("등급별 override의 grade·discountType도 enum으로 해석된다")
        void should_mapGradeOverrideStringsToEnums() {
            CouponPolicyCreateCommand.GradeOverride override =
                CouponPolicyCreateCommand.GradeOverride.of("GOLD", "RATE", 10L, 5000L, 0L);

            assertThat(override.grade()).isEqualTo(MemberGrade.GOLD);
            assertThat(override.discountType()).isEqualTo(DiscountType.RATE);
        }
    }

    @Nested
    @DisplayName("적용 범위(scopeType) 생략")
    class ScopeOmitted {

        @Test
        @DisplayName("null이면 WHOLE로 본다")
        void should_defaultToWhole_when_null() {
            assertThat(command("FIXED", null).scopeType()).isEqualTo(ScopeType.WHOLE);
        }

        @Test
        @DisplayName("빈 문자열이면 WHOLE로 본다")
        void should_defaultToWhole_when_blank() {
            assertThat(command("FIXED", "   ").scopeType()).isEqualTo(ScopeType.WHOLE);
        }
    }

    @Nested
    @DisplayName("잘못된 입력은 BAD_REQUEST")
    class InvalidInput {

        @Test
        @DisplayName("지원하지 않는 discountType")
        void should_throwBadRequest_when_unknownDiscountType() {
            assertThatThrownBy(() -> command("PERCENT", "WHOLE"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("지원하지 않는 scopeType")
        void should_throwBadRequest_when_unknownScopeType() {
            assertThatThrownBy(() -> command("FIXED", "GLOBAL"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("지원하지 않는 grade")
        void should_throwBadRequest_when_unknownGrade() {
            assertThatThrownBy(() ->
                CouponPolicyCreateCommand.GradeOverride.of("PLATINUM", "FIXED", 10L, null, 0L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
