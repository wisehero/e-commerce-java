package com.commerce.infrastructure.order;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.commerce.domain.order.PaymentResult;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PgSimulatorPaymentGatewayTest {

    private static final URI PG_BASE_URL = URI.create("http://pg-simulator.test");
    private static final URI CALLBACK_URL = URI.create("http://localhost:8080/api/v1/payments/callback");

    private MockRestServiceServer server;
    private PgSimulatorPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        PgSimulatorPaymentProperties properties = new PgSimulatorPaymentProperties();
        properties.setBaseUrl(PG_BASE_URL);
        properties.setUserId("commerce-api");
        properties.setCardType(PgSimulatorPaymentProperties.CardType.SAMSUNG);
        properties.setCardNo("1234-5678-9814-1451");
        properties.setCallbackUrl(CALLBACK_URL);
        properties.setCaptureTimeout(Duration.ofMillis(50));
        properties.setPollInterval(Duration.ofMillis(1));

        RestClient.Builder builder = RestClient.builder()
            .baseUrl(properties.getBaseUrl().toString())
            .defaultHeader("X-USER-ID", properties.getUserId());
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new PgSimulatorPaymentGateway(builder.build(), properties);
    }

    @Test
    void should_returnSuccess_when_pgTransactionSucceeds() {
        expectCreateTransaction("tx-1", "000123");
        expectTransactionStatus("tx-1", "PENDING");
        expectTransactionStatus("tx-1", "CAPTURED");

        PaymentResult result = gateway.pay(123L, new Money(5000));

        assertThat(result.approved()).isTrue();
        server.verify();
    }

    @Test
    void should_returnFailure_when_pgTransactionFails() {
        expectCreateTransaction("tx-2", "123456");
        expectTransactionStatus("tx-2", "FAILED");

        PaymentResult result = gateway.pay(123456L, new Money(5000));

        assertThat(result.approved()).isFalse();
        server.verify();
    }

    @Test
    void should_returnFailure_when_pgCreateRequestFails() {
        server.expect(requestTo(PG_BASE_URL + "/api/v1/payments"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(failEnvelope("현재 서버가 불안정합니다. 잠시 후 다시 시도해주세요.")));

        PaymentResult result = gateway.pay(123L, new Money(5000));

        assertThat(result.approved()).isFalse();
        server.verify();
    }

    @Test
    void should_throw_when_pgTransactionStaysPendingUntilTimeout() {
        PgSimulatorPaymentProperties properties = timeoutProperties();
        RestClient.Builder builder = RestClient.builder()
            .baseUrl(properties.getBaseUrl().toString())
            .defaultHeader("X-USER-ID", properties.getUserId());
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new PgSimulatorPaymentGateway(builder.build(), properties);

        expectCreateTransaction("tx-3", "000123");
        expectTransactionStatus("tx-3", "PENDING");

        assertThatThrownBy(() -> gateway.pay(123L, new Money(5000)))
            .isInstanceOf(CoreException.class)
            .hasMessage("PG 결제 결과 확인 시간이 초과되었습니다.");
        server.verify();
    }

    @Test
    void should_refundCapturedTransaction() {
        expectOrderTransactions("000123", "tx-4", "CAPTURED");
        server.expect(requestTo(PG_BASE_URL + "/api/v1/payments/tx-4/refunds"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(header("X-USER-ID", "commerce-api"))
            .andExpect(content().json("""
                {
                  "amount": 5000
                }
                """))
            .andRespond(withSuccess(successEnvelope("""
                {
                  "transactionKey": "tx-4",
                  "status": "REFUNDED",
                  "refundedAmount": 5000,
                  "refundableAmount": 0,
                  "reason": "result"
                }
                """), MediaType.APPLICATION_JSON));

        gateway.refund(123L, new Money(5000));

        server.verify();
    }

    private PgSimulatorPaymentProperties timeoutProperties() {
        PgSimulatorPaymentProperties properties = new PgSimulatorPaymentProperties();
        properties.setBaseUrl(PG_BASE_URL);
        properties.setUserId("commerce-api");
        properties.setCallbackUrl(CALLBACK_URL);
        properties.setCaptureTimeout(Duration.ZERO);
        properties.setPollInterval(Duration.ofMillis(1));
        return properties;
    }

    private void expectCreateTransaction(String transactionKey, String orderId) {
        server.expect(requestTo(PG_BASE_URL + "/api/v1/payments"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(header("X-USER-ID", "commerce-api"))
            .andExpect(content().json("""
                {
                  "orderId": "%s",
                  "cardType": "SAMSUNG",
                  "cardNo": "1234-5678-9814-1451",
                  "amount": 5000,
                  "callbackUrl": "http://localhost:8080/api/v1/payments/callback"
                }
                """.formatted(orderId)))
            .andRespond(withSuccess(successEnvelope("""
                {
                  "transactionKey": "%s",
                  "status": "PENDING",
                  "refundedAmount": 0,
                  "refundableAmount": 5000,
                  "reason": null
                }
                """.formatted(transactionKey)), MediaType.APPLICATION_JSON));
    }

    private void expectTransactionStatus(String transactionKey, String status) {
        server.expect(requestTo(PG_BASE_URL + "/api/v1/payments/" + transactionKey))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andExpect(header("X-USER-ID", "commerce-api"))
            .andRespond(withSuccess(successEnvelope("""
                {
                  "transactionKey": "%s",
                  "orderId": "000123",
                  "cardType": "SAMSUNG",
                  "cardNo": "1234-5678-9814-1451",
                  "amount": 5000,
                  "refundedAmount": 0,
                  "refundableAmount": 5000,
                  "status": "%s",
                  "reason": "result"
                }
            """.formatted(transactionKey, status)), MediaType.APPLICATION_JSON));
    }

    private void expectOrderTransactions(String orderId, String transactionKey, String status) {
        server.expect(requestTo(PG_BASE_URL + "/api/v1/payments?orderId=" + orderId))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andExpect(header("X-USER-ID", "commerce-api"))
            .andRespond(withSuccess(successEnvelope("""
                {
                  "orderId": "%s",
                  "transactions": [
                    {
                      "transactionKey": "%s",
                      "status": "%s",
                      "refundedAmount": 0,
                      "refundableAmount": 5000,
                      "reason": "result"
                    }
                  ]
                }
                """.formatted(orderId, transactionKey, status)), MediaType.APPLICATION_JSON));
    }

    private String successEnvelope(String data) {
        return """
            {
              "meta": {
                "result": "SUCCESS",
                "errorCode": null,
                "message": null
              },
              "data": %s
            }
            """.formatted(data);
    }

    private String failEnvelope(String message) {
        return """
            {
              "meta": {
                "result": "FAIL",
                "errorCode": "Internal Server Error",
                "message": "%s"
              },
              "data": null
            }
            """.formatted(message);
    }
}
