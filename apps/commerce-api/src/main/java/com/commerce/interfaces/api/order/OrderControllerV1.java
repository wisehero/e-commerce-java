package com.commerce.interfaces.api.order;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.order.OrderCancelUseCase;
import com.commerce.application.order.OrderInfo;
import com.commerce.application.order.OrderPlaceUseCase;
import com.commerce.application.order.OrderQueryUseCase;
import com.commerce.interfaces.api.ApiResponse;
import com.commerce.interfaces.api.PageResponse;
import com.commerce.support.page.PageResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderControllerV1 {

    private final OrderPlaceUseCase orderPlaceUseCase;
    private final OrderCancelUseCase orderCancelUseCase;
    private final OrderQueryUseCase orderQueryUseCase;

    @Operation(summary = "주문 생성")
    @PostMapping
    public ApiResponse<OrderResponse> place(@Valid @RequestBody OrderPlaceRequest request) {
        OrderInfo info = orderPlaceUseCase.place(request.toCommand());
        return ApiResponse.success(OrderResponse.from(info));
    }

    @Operation(summary = "주문 취소")
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable Long orderId) {
        OrderInfo info = orderCancelUseCase.cancel(orderId);
        return ApiResponse.success(OrderResponse.from(info));
    }

    @Operation(summary = "주문 상세 조회")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getById(@PathVariable Long orderId) {
        OrderInfo info = orderQueryUseCase.getById(orderId);
        return ApiResponse.success(OrderResponse.from(info));
    }

    @Operation(summary = "회원 주문 목록 조회")
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getByMember(
        @RequestParam Long memberId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<OrderInfo> result = orderQueryUseCase.getByMember(memberId, page, size);
        return ApiResponse.success(PageResponse.of(result, OrderResponse::from));
    }
}
