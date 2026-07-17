package com.commerce.infrastructure.order;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "commerce.payment.pg-simulator")
public class PgSimulatorPaymentProperties {

    private URI baseUrl = URI.create("http://localhost:8082");
    private String userId = "commerce-api";
    private CardType cardType = CardType.SAMSUNG;
    private String cardNo = "1234-5678-9814-1451";
    private URI callbackUrl = URI.create("http://localhost:8080/api/v1/payments/callback");
    private Duration captureTimeout = Duration.ofSeconds(6);
    private Duration pollInterval = Duration.ofMillis(250);

    public enum CardType {
        SAMSUNG,
        KB,
        HYUNDAI
    }
}
