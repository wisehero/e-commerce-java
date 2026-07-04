package com.commerce.interfaces.api.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.commerce.application.cart.CartAddItemCommand;
import com.commerce.application.cart.CartAddItemUseCase;
import com.commerce.application.cart.CartChangeQuantityCommand;
import com.commerce.application.cart.CartChangeQuantityUseCase;
import com.commerce.application.cart.CartCheckoutCommand;
import com.commerce.application.cart.CartCheckoutUseCase;
import com.commerce.application.cart.CartClearUseCase;
import com.commerce.application.cart.CartInfo;
import com.commerce.application.cart.CartLineInfo;
import com.commerce.application.cart.CartLineStatus;
import com.commerce.application.cart.CartRemoveItemUseCase;
import com.commerce.application.cart.CartViewUseCase;
import com.commerce.application.order.OrderInfo;
import com.commerce.application.order.OrderLineInfo;
import com.commerce.domain.order.OrderStatus;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@WebMvcTest(CartControllerV1.class)
@WithMockUser(username = "1", roles = "USER")
class CartControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartViewUseCase cartViewUseCase;
    @MockitoBean
    private CartAddItemUseCase cartAddItemUseCase;
    @MockitoBean
    private CartChangeQuantityUseCase cartChangeQuantityUseCase;
    @MockitoBean
    private CartRemoveItemUseCase cartRemoveItemUseCase;
    @MockitoBean
    private CartClearUseCase cartClearUseCase;
    @MockitoBean
    private CartCheckoutUseCase cartCheckoutUseCase;

    private CartInfo sampleCart() {
        return new CartInfo(1L,
            List.of(new CartLineInfo(10L, 2, "맨투맨", "색상:빨강", 8000L, 16000L, CartLineStatus.PURCHASABLE)),
            16000L);
    }

    private OrderInfo sampleOrder() {
        return new OrderInfo(1000L, 1L, OrderStatus.PAID.name(),
            List.of(new OrderLineInfo(1L, 100L, 10L, "맨투맨", "색상:빨강", 8000L, 2, 16000L)),
            16000L, 0L, 16000L, null, 100L);
    }

    @Nested
    @DisplayName("장바구니 조회 GET /api/v1/carts")
    class View {

        @Test
        @DisplayName("memberId 요청 파라미터 없이 인증 회원의 장바구니를 반환한다")
        void should_returnCart_when_view() throws Exception {
            given(cartViewUseCase.view(1L)).willReturn(sampleCart());

            mockMvc.perform(get("/api/v1/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.cartTotal").value(16000))
                .andExpect(jsonPath("$.data.lines[0].skuId").value(10))
                .andExpect(jsonPath("$.data.lines[0].lineSubtotal").value(16000))
                .andExpect(jsonPath("$.data.lines[0].status").value("PURCHASABLE"));

            then(cartViewUseCase).should().view(1L);
        }
    }

    @Nested
    @DisplayName("담기 POST /api/v1/carts/items")
    class AddItem {

        @Test
        @DisplayName("memberId 없는 유효 요청이면 인증 회원 기준 command로 담는다")
        void should_returnCart_when_validRequest() throws Exception {
            given(cartAddItemUseCase.addItem(any())).willReturn(sampleCart());

            String body = """
                { "skuId": 10, "quantity": 2 }
                """;

            mockMvc.perform(post("/api/v1/carts/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.lines[0].skuId").value(10));

            ArgumentCaptor<CartAddItemCommand> captor = ArgumentCaptor.forClass(CartAddItemCommand.class);
            then(cartAddItemUseCase).should().addItem(captor.capture());
            assertThat(captor.getValue().memberId()).isEqualTo(1L);
            assertThat(captor.getValue().skuId()).isEqualTo(10L);
            assertThat(captor.getValue().quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("수량이 0이면 400 + FAIL, UseCase 미호출")
        void should_return400_when_quantityZero() throws Exception {
            String body = """
                { "skuId": 10, "quantity": 0 }
                """;

            mockMvc.perform(post("/api/v1/carts/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"));

            then(cartAddItemUseCase).should(never()).addItem(any());
        }

        @Test
        @DisplayName("도메인 NOT_FOUND(회원 없음) → 404 + FAIL")
        void should_return404_when_domainNotFound() throws Exception {
            willThrow(new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다."))
                .given(cartAddItemUseCase).addItem(any());

            String body = """
                { "skuId": 10, "quantity": 2 }
                """;

            mockMvc.perform(post("/api/v1/carts/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Not Found"))
                .andExpect(jsonPath("$.meta.message").value("존재하지 않는 회원입니다."));
        }
    }

    @Nested
    @DisplayName("체크아웃 POST /api/v1/carts/checkout")
    class Checkout {

        @Test
        @DisplayName("memberId 없는 유효 요청이면 인증 회원 기준 command로 체크아웃한다")
        void should_checkoutCart_when_validRequest() throws Exception {
            given(cartCheckoutUseCase.checkout(any())).willReturn(sampleOrder());

            String body = """
                { "lockMode": "optimistic" }
                """;

            mockMvc.perform(post("/api/v1/carts/checkout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1000))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.sourceCartId").value(100));

            ArgumentCaptor<CartCheckoutCommand> captor = ArgumentCaptor.forClass(CartCheckoutCommand.class);
            then(cartCheckoutUseCase).should().checkout(captor.capture());
            assertThat(captor.getValue().memberId()).isEqualTo(1L);
            assertThat(captor.getValue().lockMode()).isEqualTo("optimistic");
        }

        @Test
        @DisplayName("빈 카트이면 400 + FAIL")
        void should_return400_when_cartEmpty() throws Exception {
            given(cartCheckoutUseCase.checkout(any()))
                .willThrow(new CoreException(ErrorType.BAD_REQUEST, "장바구니가 비어 있습니다."));

            String body = """
                { "lockMode": "optimistic" }
                """;

            mockMvc.perform(post("/api/v1/carts/checkout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.message").value("장바구니가 비어 있습니다."));
        }

        @Test
        @DisplayName("lockMode가 비어 있으면 400 + FAIL, UseCase 미호출")
        void should_return400_when_lockModeBlank() throws Exception {
            String body = """
                { "lockMode": "" }
                """;

            mockMvc.perform(post("/api/v1/carts/checkout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"));

            then(cartCheckoutUseCase).should(never()).checkout(any());
        }
    }

    @Nested
    @DisplayName("수량 변경 PATCH /api/v1/carts/items/{skuId}")
    class ChangeQuantity {

        @Test
        @DisplayName("memberId 없는 유효 요청이면 인증 회원 기준 command로 수량을 변경한다")
        void should_returnCart_when_validRequest() throws Exception {
            given(cartChangeQuantityUseCase.changeQuantity(any())).willReturn(sampleCart());

            String body = """
                { "quantity": 5 }
                """;

            mockMvc.perform(patch("/api/v1/carts/items/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));

            ArgumentCaptor<CartChangeQuantityCommand> captor =
                ArgumentCaptor.forClass(CartChangeQuantityCommand.class);
            then(cartChangeQuantityUseCase).should().changeQuantity(captor.capture());
            assertThat(captor.getValue().memberId()).isEqualTo(1L);
            assertThat(captor.getValue().skuId()).isEqualTo(10L);
            assertThat(captor.getValue().quantity()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("라인 제거 DELETE /api/v1/carts/items/{skuId}")
    class RemoveItem {

        @Test
        @DisplayName("200 OK + 장바구니를 반환한다")
        void should_returnCart_when_remove() throws Exception {
            given(cartRemoveItemUseCase.removeItem(1L, 10L)).willReturn(sampleCart());

            mockMvc.perform(delete("/api/v1/carts/items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));

            then(cartRemoveItemUseCase).should().removeItem(1L, 10L);
        }
    }

    @Nested
    @DisplayName("비우기 DELETE /api/v1/carts")
    class Clear {

        @Test
        @DisplayName("200 OK + SUCCESS를 반환하고 clear를 호출한다")
        void should_clear_when_delete() throws Exception {
            mockMvc.perform(delete("/api/v1/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));

            then(cartClearUseCase).should().clear(1L);
        }
    }
}
