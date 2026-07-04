package com.commerce.support.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigTest.SecurityBoundaryController.class)
@Import({SecurityConfig.class, SecurityConfigTest.SecurityBoundaryController.class})
@ImportAutoConfiguration({
    SecurityAutoConfiguration.class,
    ServletWebSecurityAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("회원가입 API는 인증 없이 접근할 수 있다")
    void should_permitSignUpApi_withoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/members"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("문서 경로는 인증 없이 접근할 수 있다")
    void should_permitDocs_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("헬스 경로는 인증 없이 접근할 수 있다")
    void should_permitHealth_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("보호 API는 인증 없으면 401을 반환한다")
    void should_returnUnauthorized_when_protectedApiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/carts"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("보호 API는 인증이 있으면 접근할 수 있다")
    void should_permitProtectedApi_when_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/carts").with(user("member")))
            .andExpect(status().isOk());
    }

    @RestController
    static class SecurityBoundaryController {

        @PostMapping("/api/v1/members")
        void signUp() {
        }

        @GetMapping("/swagger-ui/index.html")
        void swagger() {
        }

        @GetMapping("/actuator/health")
        void health() {
        }

        @GetMapping("/api/v1/carts")
        void carts() {
        }
    }
}
