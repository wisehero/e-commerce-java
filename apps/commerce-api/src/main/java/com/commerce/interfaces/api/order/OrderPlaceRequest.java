package com.commerce.interfaces.api.order;

import java.util.List;

import com.commerce.application.order.OrderPlaceCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderPlaceRequest(
    @NotEmpty(message = "주문 항목은 최소 1개여야 합니다.")
    @Valid
    List<LineRequest> lines,

    @NotBlank(message = "재고 차감 방식(lockMode)은 필수입니다.")
    String lockMode,

    Long couponId
) {

    public OrderPlaceCommand toCommand(Long memberId) {
        return new OrderPlaceCommand(
            memberId,
            lines.stream().map(LineRequest::toCommand).toList(),
            lockMode,
            couponId
        );
    }

    public record LineRequest(
        @NotNull(message = "SKU ID는 필수입니다.")
        Long skuId,

        @Positive(message = "주문 수량은 1개 이상이어야 합니다.")
        int quantity
    ) {
        public OrderPlaceCommand.LineCommand toCommand() {
            return new OrderPlaceCommand.LineCommand(skuId, quantity);
        }
    }
}
