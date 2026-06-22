package com.commerce.interfaces.api.coupon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.commerce.application.coupon.CouponInfo;
import com.commerce.application.coupon.CouponIssueUseCase;
import com.commerce.application.coupon.CouponQueryUseCase;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;
import com.commerce.support.page.PageResult;

@WebMvcTest(CouponControllerV1.class)
class CouponControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponIssueUseCase couponIssueUseCase;

    @MockitoBean
    private CouponQueryUseCase couponQueryUseCase;

    private CouponInfo sampleCoupon() {
        ZonedDateTime issuedAt = ZonedDateTime.parse("2026-01-01T00:00:00+09:00");
        return new CouponInfo(500L, 10L, 1L, "FIXED", 1000L, null, 0L,
            "ISSUED", issuedAt, issuedAt.plusDays(7), null, false);
    }

    @Nested
    @DisplayName("회원 쿠폰 목록 GET /api/v1/members/{memberId}/coupons")
    class GetByMember {

        @Test
        @DisplayName("200 OK + 페이지 목록을 반환한다")
        void should_returnPage_when_getByMember() throws Exception {
            given(couponQueryUseCase.getByMember(anyLong(), any(), anyInt(), anyInt()))
                .willReturn(new PageResult<>(List.of(sampleCoupon()), 1, 0, 20));

            mockMvc.perform(get("/api/v1/members/1/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].id").value(500))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("page가 정수가 아니면 400 — 타입 불일치도 BAD_REQUEST로 잡고 UseCase 미호출")
        void should_return400_when_pageNotInteger() throws Exception {
            mockMvc.perform(get("/api/v1/members/1/coupons").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"));

            then(couponQueryUseCase).should(never()).getByMember(anyLong(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("page/size 범위 위반(PageQuery 거부) → 400 + 메시지 전달")
        void should_return400_when_pageOutOfRange() throws Exception {
            willThrow(new CoreException(ErrorType.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다."))
                .given(couponQueryUseCase).getByMember(anyLong(), any(), anyInt(), anyInt());

            mockMvc.perform(get("/api/v1/members/1/coupons").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"))
                .andExpect(jsonPath("$.meta.message").value("페이지 번호는 0 이상이어야 합니다."));
        }
    }
}
