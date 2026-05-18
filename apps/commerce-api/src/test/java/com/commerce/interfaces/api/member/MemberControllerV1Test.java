package com.commerce.interfaces.api.member;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.commerce.application.member.MemberInfo;
import com.commerce.application.member.MemberSignUpUseCase;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(MemberControllerV1.class)
class MemberControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberSignUpUseCase memberSignUpUseCase;

    @Test
    @DisplayName("유효한 요청이면 200 OK + ApiResponse.success를 반환한다")
    void should_returnApiResponseSuccess_when_validRequest() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user@example.com", "password123", "오딘");
        given(memberSignUpUseCase.signUp(any()))
            .willReturn(new MemberInfo(1L, "user@example.com", "오딘", "USER"));

        // when & then
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.nickname").value("오딘"))
            .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("이메일이 공백이면 400 + ApiResponse.fail")
    void should_returnBadRequest_when_emailBlank() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("", "password123", "오딘");

        // when & then
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.meta.result").value("FAIL"))
            .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"))
            .andExpect(jsonPath("$.meta.message").value(containsString("email")));
    }

    @Test
    @DisplayName("UseCase가 CONFLICT를 던지면 409 + ApiResponse.fail")
    void should_returnConflict_when_useCaseThrowsConflict() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user@example.com", "password123", "오딘");
        given(memberSignUpUseCase.signUp(any()))
            .willThrow(new CoreException(ErrorType.CONFLICT, "이미 가입된 이메일입니다."));

        // when & then
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.meta.result").value("FAIL"))
            .andExpect(jsonPath("$.meta.errorCode").value("Conflict"))
            .andExpect(jsonPath("$.meta.message").value("이미 가입된 이메일입니다."));
    }

    @Test
    @DisplayName("UseCase가 BAD_REQUEST를 던지면 400 + ApiResponse.fail")
    void should_returnBadRequest_when_useCaseThrowsBadRequest() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("invalid-email", "password123", "오딘");
        given(memberSignUpUseCase.signUp(any()))
            .willThrow(new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."));

        // when & then
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.meta.result").value("FAIL"))
            .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"));
    }
}
