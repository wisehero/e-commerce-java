package com.commerce.interfaces.api.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.commerce.application.coupon.CouponInfo;
import com.commerce.application.coupon.CouponIssueCommand;
import com.commerce.application.coupon.CouponIssueUseCase;
import com.commerce.application.coupon.CouponQueryUseCase;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;
import com.commerce.support.page.PageResult;

@WebMvcTest(CouponControllerV1.class)
@WithMockUser(username = "1", roles = "USER")
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
    @DisplayName("쿠폰 발급 POST /api/v1/coupons")
    class Issue {

        @Test
        @DisplayName("memberId 없는 유효 요청이면 인증 회원 기준 command로 발급한다")
        void should_issueCoupon_when_validRequest() throws Exception {
            given(couponIssueUseCase.issue(any())).willReturn(sampleCoupon());

            String body = """
                { "policyId": 10 }
                """;

            mockMvc.perform(post("/api/v1/coupons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(500));

            ArgumentCaptor<CouponIssueCommand> captor = ArgumentCaptor.forClass(CouponIssueCommand.class);
            then(couponIssueUseCase).should().issue(captor.capture());
            assertThat(captor.getValue().policyId()).isEqualTo(10L);
            assertThat(captor.getValue().memberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("policyId가 없으면 400 + FAIL, UseCase 미호출")
        void should_return400_when_policyIdMissing() throws Exception {
            mockMvc.perform(post("/api/v1/coupons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"));

            then(couponIssueUseCase).should(never()).issue(any());
        }
    }

    @Nested
    @DisplayName("내 쿠폰 목록 GET /api/v1/coupons/me")
    class GetByMember {

        @Test
        @DisplayName("200 OK + 페이지 목록을 반환한다")
        void should_returnPage_when_getByMember() throws Exception {
            given(couponQueryUseCase.getByMember(1L, null, 0, 20))
                .willReturn(new PageResult<>(List.of(sampleCoupon()), 1, 0, 20));

            mockMvc.perform(get("/api/v1/coupons/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].id").value(500))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));

            then(couponQueryUseCase).should().getByMember(1L, null, 0, 20);
        }

        @Test
        @DisplayName("page가 정수가 아니면 400 — 타입 불일치도 BAD_REQUEST로 잡고 UseCase 미호출")
        void should_return400_when_pageNotInteger() throws Exception {
            mockMvc.perform(get("/api/v1/coupons/me").param("page", "abc"))
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

            mockMvc.perform(get("/api/v1/coupons/me").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"))
                .andExpect(jsonPath("$.meta.message").value("페이지 번호는 0 이상이어야 합니다."));
        }
    }
}
