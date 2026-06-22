package com.commerce.interfaces.api.order;

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

import com.commerce.application.order.OrderCancelUseCase;
import com.commerce.application.order.OrderInfo;
import com.commerce.application.order.OrderLineInfo;
import com.commerce.application.order.OrderPlaceUseCase;
import com.commerce.application.order.OrderQueryUseCase;
import com.commerce.domain.order.OrderStatus;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;
import com.commerce.support.page.PageResult;

@WebMvcTest(OrderControllerV1.class)
class OrderControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderPlaceUseCase orderPlaceUseCase;

    @MockitoBean
    private OrderCancelUseCase orderCancelUseCase;

    @MockitoBean
    private OrderQueryUseCase orderQueryUseCase;

    private OrderInfo sampleOrder(OrderStatus status) {
        return new OrderInfo(1000L, 1L, status.name(),
            List.of(new OrderLineInfo(1L, 100L, 10L, "맨투맨", "색상:빨강", 8000L, 2, 16000L)),
            16000L);
    }

    @Nested
    @DisplayName("주문 생성 POST /api/v1/orders")
    class Place {

        @Test
        @DisplayName("유효 요청이면 200 OK + 생성된 주문을 반환한다")
        void should_returnOrder_when_validRequest() throws Exception {
            given(orderPlaceUseCase.place(any())).willReturn(sampleOrder(OrderStatus.PAID));

            String body = """
                {
                  "memberId": 1,
                  "lockMode": "optimistic",
                  "lines": [ { "skuId": 10, "quantity": 2 } ]
                }
                """;

            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1000))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.totalAmount").value(16000))
                .andExpect(jsonPath("$.data.lines[0].lineAmount").value(16000));
        }

        @Test
        @DisplayName("주문 항목이 비어 있으면 400 + FAIL, UseCase 미호출")
        void should_return400_when_linesEmpty() throws Exception {
            String body = """
                {
                  "memberId": 1,
                  "lockMode": "optimistic",
                  "lines": []
                }
                """;

            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"));

            then(orderPlaceUseCase).should(never()).place(any());
        }

        @Test
        @DisplayName("lockMode가 비어 있으면 400 + FAIL")
        void should_return400_when_lockModeBlank() throws Exception {
            String body = """
                {
                  "memberId": 1,
                  "lockMode": "",
                  "lines": [ { "skuId": 10, "quantity": 2 } ]
                }
                """;

            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"));

            then(orderPlaceUseCase).should(never()).place(any());
        }

        @Test
        @DisplayName("재고 부족 등 도메인 BAD_REQUEST → 400")
        void should_return400_when_domainRejects() throws Exception {
            given(orderPlaceUseCase.place(any()))
                .willThrow(new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다."));

            String body = """
                {
                  "memberId": 1,
                  "lockMode": "optimistic",
                  "lines": [ { "skuId": 10, "quantity": 2 } ]
                }
                """;

            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"))
                .andExpect(jsonPath("$.meta.message").value("재고가 부족합니다."));
        }
    }

    @Nested
    @DisplayName("주문 취소 POST /api/v1/orders/{id}/cancel")
    class Cancel {

        @Test
        @DisplayName("200 OK + 취소된 주문을 반환한다")
        void should_returnCancelledOrder_when_cancel() throws Exception {
            given(orderCancelUseCase.cancel(1000L)).willReturn(sampleOrder(OrderStatus.CANCELLED));

            mockMvc.perform(post("/api/v1/orders/1000/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

            then(orderCancelUseCase).should().cancel(1000L);
        }
    }

    @Nested
    @DisplayName("단건 조회 GET /api/v1/orders/{id}")
    class GetById {

        @Test
        @DisplayName("200 OK + 주문을 반환한다")
        void should_returnOrder_when_found() throws Exception {
            given(orderQueryUseCase.getById(1000L)).willReturn(sampleOrder(OrderStatus.PAID));

            mockMvc.perform(get("/api/v1/orders/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1000));
        }

        @Test
        @DisplayName("존재하지 않으면 404 + FAIL")
        void should_return404_when_notFound() throws Exception {
            given(orderQueryUseCase.getById(anyLong()))
                .willThrow(new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));

            mockMvc.perform(get("/api/v1/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Not Found"))
                .andExpect(jsonPath("$.meta.message").value("존재하지 않는 주문입니다."));
        }
    }

    @Nested
    @DisplayName("회원별 목록 GET /api/v1/orders")
    class GetByMember {

        @Test
        @DisplayName("200 OK + 페이지 목록을 반환한다")
        void should_returnPage_when_getByMember() throws Exception {
            given(orderQueryUseCase.getByMember(anyLong(), anyInt(), anyInt()))
                .willReturn(new PageResult<>(List.of(sampleOrder(OrderStatus.PAID)), 1, 0, 20));

            mockMvc.perform(get("/api/v1/orders").param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].id").value(1000))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("memberId가 없으면 400")
        void should_return400_when_memberIdMissing() throws Exception {
            mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("page가 정수가 아니면 400 — 타입 불일치도 BAD_REQUEST로 잡고 UseCase 미호출")
        void should_return400_when_pageNotInteger() throws Exception {
            mockMvc.perform(get("/api/v1/orders")
                    .param("memberId", "1")
                    .param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("Bad Request"));

            then(orderQueryUseCase).should(never()).getByMember(anyLong(), anyInt(), anyInt());
        }
    }
}
