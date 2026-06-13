package com.commerce.application.product;

import java.util.List;

public record ProductRegisterCommand(
    String name,
    String description,
    Long categoryId,
    Long brandId,
    String imageUrl,
    List<SkuRegisterCommand> skus
) {

    public record SkuRegisterCommand(
        List<OptionValueCommand> optionValues,
        long originalPrice,
        int stock
    ) {
    }

    public record OptionValueCommand(
        String name,
        String value
    ) {
    }
}
