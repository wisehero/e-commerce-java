package com.commerce.config.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.EnumFeature;

@Configuration
class JacksonConfig {

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Default inclusion - skip null fields
            builder.changeDefaultPropertyInclusion(
                handler -> handler.withValueInclusion(JsonInclude.Include.NON_NULL)
            );

            // Stream write features (Jackson 3에서 JsonGenerator.Feature → StreamWriteFeature)
            builder.enable(
                StreamWriteFeature.AUTO_CLOSE_CONTENT,
                StreamWriteFeature.IGNORE_UNKNOWN,
                StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN
            );

            // Serialization features
            builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

            // Deserialization features
            builder.enable(
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
            );
            builder.disable(
                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            );

            // Enum features (Jackson 3에서 DeserializationFeature → EnumFeature 로 이동)
            builder.enable(EnumFeature.READ_ENUMS_USING_TO_STRING);
            builder.disable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        };
    }
}
