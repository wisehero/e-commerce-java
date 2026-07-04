package com.commerce.support.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    @DisplayName("로그인과 refresh 예정 경로는 인증 없이 접근할 수 있다")
    void should_permitAuthApi_withoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("publicCatalogRequests")
    @DisplayName("고객 카탈로그 조회 API는 인증 없이 접근할 수 있다")
    void should_permitCatalogReadApi_withoutAuthentication(RequestBuilder requestBuilder) throws Exception {
        mockMvc.perform(requestBuilder)
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
        mockMvc.perform(get("/api/v1/carts").with(user("1").roles("USER")))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("adminRequests")
    @DisplayName("ADMIN API는 인증 없으면 401을 반환한다")
    void should_returnUnauthorized_when_adminApiWithoutAuthentication(RequestBuilder requestBuilder) throws Exception {
        mockMvc.perform(requestBuilder)
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("adminRequests")
    @DisplayName("ADMIN API는 USER 권한이면 403을 반환한다")
    void should_returnForbidden_when_adminApiWithUserRole(RequestBuilder requestBuilder) throws Exception {
        mockMvc.perform(withUser(requestBuilder))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("adminRequests")
    @DisplayName("ADMIN API는 ADMIN 권한이면 접근할 수 있다")
    void should_permitAdminApi_when_adminRole(RequestBuilder requestBuilder) throws Exception {
        mockMvc.perform(withAdmin(requestBuilder))
            .andExpect(status().isOk());
    }

    private static Stream<RequestBuilder> publicCatalogRequests() {
        return Stream.of(
            get("/api/v1/products"),
            get("/api/v1/products/1"),
            get("/api/v1/brands/1"),
            get("/api/v1/categories"),
            get("/api/v1/categories/1")
        );
    }

    private static Stream<RequestBuilder> adminRequests() {
        return Stream.of(
            post("/api/v1/admin/products"),
            post("/api/v1/admin/products/1/suspend"),
            patch("/api/v1/admin/skus/1/discount"),
            patch("/api/v1/admin/skus/1/price"),
            patch("/api/v1/admin/skus/1/stock"),
            post("/api/v1/admin/brands"),
            patch("/api/v1/admin/brands/1"),
            post("/api/v1/admin/brands/1/deactivate"),
            post("/api/v1/admin/categories"),
            patch("/api/v1/admin/categories/1"),
            post("/api/v1/admin/categories/1/deactivate"),
            delete("/api/v1/admin/categories/1"),
            post("/api/v1/admin/coupon-policies")
        );
    }

    private static MockHttpServletRequestBuilder withUser(RequestBuilder requestBuilder) {
        return withRole(requestBuilder, "USER");
    }

    private static MockHttpServletRequestBuilder withAdmin(RequestBuilder requestBuilder) {
        return withRole(requestBuilder, "ADMIN");
    }

    private static MockHttpServletRequestBuilder withRole(RequestBuilder requestBuilder, String role) {
        MockHttpServletRequestBuilder builder = (MockHttpServletRequestBuilder) requestBuilder;
        return builder.with(user("1").roles(role));
    }

    @RestController
    static class SecurityBoundaryController {

        @PostMapping("/api/v1/members")
        void signUp() {
        }

        @PostMapping("/api/v1/auth/login")
        void login() {
        }

        @PostMapping("/api/v1/auth/refresh")
        void refresh() {
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

        @GetMapping("/api/v1/products")
        void products() {
        }

        @GetMapping("/api/v1/products/{productId}")
        void productDetail() {
        }

        @PostMapping("/api/v1/admin/products")
        void productRegister() {
        }

        @PostMapping("/api/v1/admin/products/{productId}/suspend")
        void productSuspend() {
        }

        @PatchMapping("/api/v1/admin/skus/{skuId}/discount")
        void skuDiscount() {
        }

        @PatchMapping("/api/v1/admin/skus/{skuId}/price")
        void skuPrice() {
        }

        @PatchMapping("/api/v1/admin/skus/{skuId}/stock")
        void skuStock() {
        }

        @GetMapping("/api/v1/brands/{brandId}")
        void brandDetail() {
        }

        @PostMapping("/api/v1/admin/brands")
        void brandRegister() {
        }

        @PatchMapping("/api/v1/admin/brands/{brandId}")
        void brandUpdate() {
        }

        @PostMapping("/api/v1/admin/brands/{brandId}/deactivate")
        void brandDeactivate() {
        }

        @GetMapping("/api/v1/categories")
        void categoryTree() {
        }

        @GetMapping("/api/v1/categories/{categoryId}")
        void categoryDetail() {
        }

        @PostMapping("/api/v1/admin/categories")
        void categoryRegister() {
        }

        @PatchMapping("/api/v1/admin/categories/{categoryId}")
        void categoryUpdate() {
        }

        @PostMapping("/api/v1/admin/categories/{categoryId}/deactivate")
        void categoryDeactivate() {
        }

        @DeleteMapping("/api/v1/admin/categories/{categoryId}")
        void categoryDelete() {
        }

        @PostMapping("/api/v1/admin/coupon-policies")
        void couponPolicyCreate() {
        }
    }
}
