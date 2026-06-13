package com.commerce.infrastructure.product;

import com.commerce.domain.product.OptionValue;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class OptionValueEmbeddable {

    @Column(name = "option_name", nullable = false, length = 50)
    private String name;

    @Column(name = "option_value", nullable = false, length = 100)
    private String value;

    protected OptionValueEmbeddable() {
    }

    private OptionValueEmbeddable(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static OptionValueEmbeddable fromDomain(OptionValue optionValue) {
        return new OptionValueEmbeddable(optionValue.name(), optionValue.value());
    }

    public OptionValue toDomain() {
        return new OptionValue(name, value);
    }
}
