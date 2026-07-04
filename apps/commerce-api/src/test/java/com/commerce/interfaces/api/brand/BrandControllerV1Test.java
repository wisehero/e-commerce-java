package com.commerce.interfaces.api.brand;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.commerce.application.brand.BrandInfo;
import com.commerce.application.brand.BrandQueryUseCase;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@WebMvcTest(BrandControllerV1.class)
class BrandControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandQueryUseCase brandQueryUseCase;

    @Nested
    @DisplayName("GET /api/v1/brands/{id}")
    class GetBrand {

        @Test
        @DisplayName("ACTIVE 브랜드를 반환한다")
        void should_returnBrand_when_found() throws Exception {
            given(brandQueryUseCase.getBrand(1L))
                .willReturn(new BrandInfo(1L, "나이키", "logo.jpg", "ACTIVE"));

            mockMvc.perform(get("/api/v1/brands/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("나이키"));
        }

        @Test
        @DisplayName("없거나 비활성이면 404를 반환한다")
        void should_return404_when_notFound() throws Exception {
            given(brandQueryUseCase.getBrand(anyLong()))
                .willThrow(new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));

            mockMvc.perform(get("/api/v1/brands/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.result").value("FAIL"));
        }
    }
}
