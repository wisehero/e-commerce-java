package com.commerce.interfaces.api.product;

import static com.commerce.application.product.ProductRegisterCommand.*;

import java.util.List;

import com.commerce.application.product.ProductRegisterCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductRegisterRequest(
    @NotBlank(message = "상품명은 필수입니다.")
    String name,

    String description,

    @NotNull(message = "카테고리는 필수입니다.")
    Long categoryId,

    Long brandId,

    String imageUrl,

    @NotEmpty(message = "옵션(SKU)은 최소 1개여야 합니다.")
    @Valid
    List<SkuRequest> skus
) {

    public ProductRegisterCommand toCommand() {
        return new ProductRegisterCommand(
            name, description, categoryId, brandId, imageUrl,
            skus.stream().map(SkuRequest::toCommand).toList()
        );
    }

    public record SkuRequest(
        @NotEmpty(message = "옵션값은 최소 1개여야 합니다.")
        @Valid
        List<OptionValueRequest> optionValues,

        @PositiveOrZero(message = "정가는 0 이상이어야 합니다.")
        long originalPrice,

        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        int stock
    ) {
        public SkuRegisterCommand toCommand() {
            return new SkuRegisterCommand(
                optionValues.stream().map(OptionValueRequest::toCommand).toList(),
                originalPrice, stock
            );
        }
    }

    public record OptionValueRequest(
        @NotBlank(message = "옵션명은 필수입니다.")
        String name,

        @NotBlank(message = "옵션값은 필수입니다.")
        String value
    ) {
        public OptionValueCommand toCommand() {
            return new OptionValueCommand(name, value);
        }
    }
}
