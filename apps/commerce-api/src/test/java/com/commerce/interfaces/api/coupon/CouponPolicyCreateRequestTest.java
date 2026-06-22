package com.commerce.interfaces.api.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.commerce.application.coupon.CouponPolicyCreateCommand;
import com.commerce.domain.coupon.DiscountType;
import com.commerce.domain.coupon.ScopeType;
import com.commerce.domain.member.MemberGrade;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@DisplayName("CouponPolicyCreateRequest.toCommand() - raw string 위임")
class CouponPolicyCreateRequestTest {

    private static final ZonedDateTime FROM = ZonedDateTime.parse("2026-01-01T00:00:00+09:00");
    private static final ZonedDateTime UNTIL = ZonedDateTime.parse("2026-12-31T23:59:59+09:00");

    private static CouponPolicyCreateRequest request(
        String discountType,
        String scopeType,
        List<CouponPolicyCreateRequest.GradeOverrideRequest> gradeOverrides
    ) {
        return new CouponPolicyCreateRequest(
            "신규가입 할인", discountType, 1000L, null, 0L,
            scopeType, null, gradeOverrides,
            7, FROM, UNTIL, 100L, true);
    }

    @Test
    @DisplayName("문자열 필드가 도메인 enum을 보유한 Command로 변환된다")
    void should_convertStringsToEnumCommand() {
        CouponPolicyCreateRequest request = request("RATE", "PRODUCT",
            List.of(new CouponPolicyCreateRequest.GradeOverrideRequest("VIP", "FIXED", 2000L, null, 0L)));

        CouponPolicyCreateCommand command = request.toCommand();

        assertThat(command.discountType()).isEqualTo(DiscountType.RATE);
        assertThat(command.scopeType()).isEqualTo(ScopeType.PRODUCT);
        assertThat(command.gradeOverrides()).singleElement()
            .satisfies(o -> {
                assertThat(o.grade()).isEqualTo(MemberGrade.VIP);
                assertThat(o.discountType()).isEqualTo(DiscountType.FIXED);
            });
    }

    @Test
    @DisplayName("scopeType이 null이면 WHOLE로 변환된다")
    void should_defaultScopeToWhole_when_null() {
        CouponPolicyCreateCommand command = request("FIXED", null, List.of()).toCommand();

        assertThat(command.scopeType()).isEqualTo(ScopeType.WHOLE);
    }

    @Test
    @DisplayName("잘못된 문자열은 팩토리로 위임되어 BAD_REQUEST가 된다")
    void should_throwBadRequest_when_unknownDiscountType() {
        CouponPolicyCreateRequest request = request("PERCENT", "WHOLE", List.of());

        assertThatThrownBy(request::toCommand)
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
    }
}
