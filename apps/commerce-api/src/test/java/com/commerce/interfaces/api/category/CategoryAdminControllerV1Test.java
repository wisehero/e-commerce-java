package com.commerce.interfaces.api.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.commerce.application.category.CategoryDeleteUseCase;
import com.commerce.application.category.CategoryInfo;
import com.commerce.application.category.CategoryRegisterCommand;
import com.commerce.application.category.CategoryRegisterUseCase;
import com.commerce.application.category.CategoryStatusChangeUseCase;
import com.commerce.application.category.CategoryUpdateCommand;
import com.commerce.application.category.CategoryUpdateUseCase;

@WebMvcTest(CategoryAdminControllerV1.class)
class CategoryAdminControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRegisterUseCase categoryRegisterUseCase;

    @MockitoBean
    private CategoryUpdateUseCase categoryUpdateUseCase;

    @MockitoBean
    private CategoryStatusChangeUseCase categoryStatusChangeUseCase;

    @MockitoBean
    private CategoryDeleteUseCase categoryDeleteUseCase;

    @Test
    @DisplayName("POST /api/v1/admin/categories는 카테고리를 등록한다")
    void should_registerCategory_when_validRequest() throws Exception {
        given(categoryRegisterUseCase.register(any()))
            .willReturn(new CategoryInfo(1L, "상의", null, 0, 10, "ACTIVE"));

        mockMvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "상의", "parentId": null, "sortOrder": 10 }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.name").value("상의"));

        ArgumentCaptor<CategoryRegisterCommand> captor = ArgumentCaptor.forClass(CategoryRegisterCommand.class);
        then(categoryRegisterUseCase).should().register(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("상의");
        assertThat(captor.getValue().sortOrder()).isEqualTo(10);
    }

    @Test
    @DisplayName("POST /api/v1/admin/categories는 이름이 공백이면 400을 반환한다")
    void should_return400_when_nameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "", "parentId": null, "sortOrder": 10 }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.meta.result").value("FAIL"));

        then(categoryRegisterUseCase).should(never()).register(any());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/categories/{id}는 카테고리를 수정한다")
    void should_updateCategory_when_validRequest() throws Exception {
        given(categoryUpdateUseCase.update(any()))
            .willReturn(new CategoryInfo(1L, "아우터", null, 0, 20, "ACTIVE"));

        mockMvc.perform(patch("/api/v1/admin/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "아우터", "sortOrder": 20 }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("아우터"));

        ArgumentCaptor<CategoryUpdateCommand> captor = ArgumentCaptor.forClass(CategoryUpdateCommand.class);
        then(categoryUpdateUseCase).should().update(captor.capture());
        assertThat(captor.getValue().categoryId()).isEqualTo(1L);
        assertThat(captor.getValue().name()).isEqualTo("아우터");
    }

    @Test
    @DisplayName("상태 전이와 삭제 API는 UseCase에 위임한다")
    void should_delegateStatusChangeAndDelete() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories/1/deactivate"))
            .andExpect(status().isOk());
        then(categoryStatusChangeUseCase).should().deactivate(1L);

        mockMvc.perform(post("/api/v1/admin/categories/1/activate"))
            .andExpect(status().isOk());
        then(categoryStatusChangeUseCase).should().activate(1L);

        mockMvc.perform(delete("/api/v1/admin/categories/1"))
            .andExpect(status().isOk());
        then(categoryDeleteUseCase).should().delete(1L);
    }
}
