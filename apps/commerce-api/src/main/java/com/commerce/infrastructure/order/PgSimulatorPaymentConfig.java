package com.commerce.infrastructure.order;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PgSimulatorPaymentProperties.class)
class PgSimulatorPaymentConfig {

    @Bean
    @Qualifier("pgSimulatorRestClient")
    @ConditionalOnProperty(name = "commerce.payment.gateway", havingValue = "pg-simulator", matchIfMissing = true)
    RestClient pgSimulatorRestClient(PgSimulatorPaymentProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.getBaseUrl().toString())
            .defaultHeader("X-USER-ID", properties.getUserId())
            .build();
    }
}
