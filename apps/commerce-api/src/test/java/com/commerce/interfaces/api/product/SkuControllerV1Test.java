package com.commerce.interfaces.api.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.commerce.application.product.SkuPriceChangeUseCase;
import com.commerce.application.product.SkuStockAdjustUseCase;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@WebMvcTest(SkuControllerV1.class)
class SkuControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkuPriceChangeUseCase skuPriceChangeUseCase;

    @MockitoBean
    private SkuStockAdjustUseCase skuStockAdjustUseCase;

    @Nested
    @DisplayName("할인 적용 PATCH /api/v1/skus/{id}/discount")
    class ApplyDiscount {

        @Test
        @DisplayName("유효 요청이면 200 + UseCase에 위임한다")
        void should_delegate_when_valid() throws Exception {
            mockMvc.perform(patch("/api/v1/skus/1/discount")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"salePrice\": 9000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));

            then(skuPriceChangeUseCase).should().applyDiscount(any());
        }

        @Test
        @DisplayName("할인가가 음수면 400 + UseCase 미호출")
        void should_return400_when_negative() throws Exception {
            mockMvc.perform(patch("/api/v1/skus/1/discount")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"salePrice\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"));

            then(skuPriceChangeUseCase).should(never()).applyDiscount(any());
        }

        @Test
        @DisplayName("할인가 > 정가면 도메인 BAD_REQUEST → 400")
        void should_return400_when_domainRejects() throws Exception {
            willThrow(new CoreException(ErrorType.BAD_REQUEST, "할인가는 정가보다 클 수 없습니다."))
                .given(skuPriceChangeUseCase).applyDiscount(any());

            mockMvc.perform(patch("/api/v1/skus/1/discount")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"salePrice\": 9999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.message").value("할인가는 정가보다 클 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("정가 변경 PATCH /api/v1/skus/{id}/price")
    class ChangePrice {

        @Test
        @DisplayName("유효 요청이면 200 + 위임")
        void should_delegate_when_valid() throws Exception {
            mockMvc.perform(patch("/api/v1/skus/1/price")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"originalPrice\": 12000}"))
                .andExpect(status().isOk());

            then(skuPriceChangeUseCase).should().changePrice(any());
        }

        @Test
        @DisplayName("존재하지 않는 SKU면 404")
        void should_return404_when_notFound() throws Exception {
            willThrow(new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 옵션(SKU)입니다."))
                .given(skuPriceChangeUseCase).changePrice(any());

            mockMvc.perform(patch("/api/v1/skus/999/price")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"originalPrice\": 12000}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.errorCode").value("Not Found"));
        }
    }

    @Nested
    @DisplayName("재고 입고 PATCH /api/v1/skus/{id}/stock")
    class Restock {

        @Test
        @DisplayName("유효 요청이면 200 + 위임")
        void should_delegate_when_valid() throws Exception {
            mockMvc.perform(patch("/api/v1/skus/1/stock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\": 50}"))
                .andExpect(status().isOk());

            then(skuStockAdjustUseCase).should().restock(any());
        }

        @Test
        @DisplayName("입고 수량이 0 이하면 400 + UseCase 미호출")
        void should_return400_when_nonPositive() throws Exception {
            mockMvc.perform(patch("/api/v1/skus/1/stock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"));

            then(skuStockAdjustUseCase).should(never()).restock(any());
        }
    }
}
