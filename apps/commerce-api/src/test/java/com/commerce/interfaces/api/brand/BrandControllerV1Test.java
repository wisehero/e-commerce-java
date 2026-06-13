package com.commerce.interfaces.api.brand;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.commerce.application.brand.BrandInfo;
import com.commerce.application.brand.BrandQueryUseCase;
import com.commerce.application.brand.BrandRegisterUseCase;
import com.commerce.application.brand.BrandStatusChangeUseCase;
import com.commerce.application.brand.BrandUpdateUseCase;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@WebMvcTest(BrandControllerV1.class)
class BrandControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandQueryUseCase brandQueryUseCase;

    @MockitoBean
    private BrandRegisterUseCase brandRegisterUseCase;

    @MockitoBean
    private BrandUpdateUseCase brandUpdateUseCase;

    @MockitoBean
    private BrandStatusChangeUseCase brandStatusChangeUseCase;

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

    @Nested
    @DisplayName("POST /api/v1/brands")
    class Register {

        @Test
        @DisplayName("유효 요청이면 브랜드를 등록한다")
        void should_registerBrand_when_validRequest() throws Exception {
            given(brandRegisterUseCase.register(any()))
                .willReturn(new BrandInfo(1L, "나이키", "logo.jpg", "ACTIVE"));

            String body = """
                { "name": "나이키", "logoUrl": "logo.jpg" }
                """;

            mockMvc.perform(post("/api/v1/brands")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("나이키"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("브랜드명이 공백이면 400을 반환하고 UseCase를 호출하지 않는다")
        void should_return400_when_nameBlank() throws Exception {
            String body = """
                { "name": "", "logoUrl": "logo.jpg" }
                """;

            mockMvc.perform(post("/api/v1/brands")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"));

            then(brandRegisterUseCase).should(never()).register(any());
        }
    }

    @Test
    @DisplayName("PATCH /api/v1/brands/{id}는 브랜드를 수정한다")
    void should_updateBrand_when_patch() throws Exception {
        given(brandUpdateUseCase.update(any()))
            .willReturn(new BrandInfo(1L, "아디다스", "new.jpg", "ACTIVE"));

        String body = """
            { "name": "아디다스", "logoUrl": "new.jpg" }
            """;

        mockMvc.perform(patch("/api/v1/brands/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("아디다스"));
    }

    @Test
    @DisplayName("상태 전이 API는 UseCase에 위임한다")
    void should_delegateStatusChange() throws Exception {
        mockMvc.perform(post("/api/v1/brands/1/deactivate"))
            .andExpect(status().isOk());
        then(brandStatusChangeUseCase).should().deactivate(1L);

        mockMvc.perform(post("/api/v1/brands/1/activate"))
            .andExpect(status().isOk());
        then(brandStatusChangeUseCase).should().activate(1L);
    }
}
