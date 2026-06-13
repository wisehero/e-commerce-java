package com.commerce.interfaces.api.product;

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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.commerce.application.product.ProductDetailInfo;
import com.commerce.application.product.ProductDetailQueryUseCase;
import com.commerce.application.product.ProductRegisterUseCase;
import com.commerce.application.product.ProductSearchUseCase;
import com.commerce.application.product.ProductStatusChangeUseCase;
import com.commerce.application.product.ProductSummaryInfo;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;
import com.commerce.support.page.PageResult;

@WebMvcTest(ProductControllerV1.class)
class ProductControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRegisterUseCase productRegisterUseCase;

    @MockitoBean
    private ProductDetailQueryUseCase productDetailQueryUseCase;

    @MockitoBean
    private ProductSearchUseCase productSearchUseCase;

    @MockitoBean
    private ProductStatusChangeUseCase productStatusChangeUseCase;

    private ProductDetailInfo sampleDetail() {
        return new ProductDetailInfo(
            1L, "맥북 프로", "M4 칩", 10L, 20L, "애플", "logo.jpg", "img.jpg", "ON_SALE",
            List.of(new ProductDetailInfo.SkuInfo(
                100L,
                List.of(new ProductDetailInfo.OptionValueInfo("색상", "스페이스블랙")),
                3000000L, 2700000L, 10, true))
        );
    }

    @Nested
    @DisplayName("목록/검색 GET /api/v1/products")
    class Search {

        @Test
        @DisplayName("200 OK + 페이지 요약 목록을 반환한다")
        void should_returnSummaryPage_when_search() throws Exception {
            given(productSearchUseCase.search(any(), any(), any(), anyInt(), anyInt()))
                .willReturn(new PageResult<>(
                    List.of(new ProductSummaryInfo(1L, "맥북 프로", "img.jpg", 2700000L)), 1, 0, 20));

            mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].lowestSalePrice").value(2700000))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
        }
    }

    @Nested
    @DisplayName("상세 GET /api/v1/products/{id}")
    class Detail {

        @Test
        @DisplayName("200 OK + 상세를 반환한다")
        void should_returnDetail_when_found() throws Exception {
            given(productDetailQueryUseCase.getDetail(1L)).willReturn(sampleDetail());

            mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.brandName").value("애플"))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.skus[0].discounted").value(true));
        }

        @Test
        @DisplayName("존재하지 않으면 404 + FAIL")
        void should_return404_when_notFound() throws Exception {
            given(productDetailQueryUseCase.getDetail(anyLong()))
                .willThrow(new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));

            mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Not Found"))
                .andExpect(jsonPath("$.meta.message").value("존재하지 않는 상품입니다."));
        }
    }

    @Nested
    @DisplayName("등록 POST /api/v1/products")
    class Register {

        @Test
        @DisplayName("유효 요청이면 200 OK + 등록된 상세를 반환한다")
        void should_returnDetail_when_validRequest() throws Exception {
            given(productRegisterUseCase.register(any())).willReturn(sampleDetail());

            String body = """
                {
                  "name": "맥북 프로",
                  "description": "M4 칩",
                  "categoryId": 10,
                  "brandId": 20,
                  "imageUrl": "img.jpg",
                  "skus": [
                    { "optionValues": [{"name":"색상","value":"스페이스블랙"}], "originalPrice": 3000000, "stock": 10 }
                  ]
                }
                """;

            mockMvc.perform(post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("상품명이 공백이면 400 + FAIL, UseCase 미호출")
        void should_return400_when_nameBlank() throws Exception {
            String body = """
                {
                  "name": "",
                  "categoryId": 10,
                  "brandId": 20,
                  "skus": [
                    { "optionValues": [{"name":"색상","value":"블랙"}], "originalPrice": 3000000, "stock": 10 }
                  ]
                }
                """;

            mockMvc.perform(post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"));

            then(productRegisterUseCase).should(never()).register(any());
        }
    }

    @Nested
    @DisplayName("상태 전이 POST /api/v1/products/{id}/{action}")
    class StatusTransition {

        @Test
        @DisplayName("suspend 호출 시 200 OK + UseCase에 위임한다")
        void should_delegateSuspend() throws Exception {
            mockMvc.perform(post("/api/v1/products/1/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));

            then(productStatusChangeUseCase).should().suspend(1L);
        }

        @Test
        @DisplayName("resume 호출 시 200 OK + UseCase에 위임한다")
        void should_delegateResume() throws Exception {
            mockMvc.perform(post("/api/v1/products/1/resume"))
                .andExpect(status().isOk());

            then(productStatusChangeUseCase).should().resume(1L);
        }

        @Test
        @DisplayName("discontinue 호출 시 200 OK + UseCase에 위임한다")
        void should_delegateDiscontinue() throws Exception {
            mockMvc.perform(post("/api/v1/products/1/discontinue"))
                .andExpect(status().isOk());

            then(productStatusChangeUseCase).should().discontinue(1L);
        }

        @Test
        @DisplayName("전이 불가(단종 후 재개 등) 시 도메인 BAD_REQUEST → 400")
        void should_return400_when_invalidTransition() throws Exception {
            willThrow(new CoreException(ErrorType.BAD_REQUEST, "이미 단종된 상품은 재개할 수 없습니다."))
                .given(productStatusChangeUseCase).resume(anyLong());

            mockMvc.perform(post("/api/v1/products/1/resume"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"));
        }
    }
}
