package com.commerce.infrastructure.order;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.commerce.domain.order.PaymentGateway;
import com.commerce.domain.order.PaymentResult;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "commerce.payment.gateway", havingValue = "pg-simulator", matchIfMissing = true)
public class PgSimulatorPaymentGateway implements PaymentGateway {

    private static final ParameterizedTypeReference<PgEnvelope<PgTransaction>> TRANSACTION_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<PgEnvelope<PgTransactionDetail>> DETAIL_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<PgEnvelope<PgOrderTransactions>> ORDER_TYPE =
        new ParameterizedTypeReference<>() {
        };

    private final RestClient restClient;
    private final PgSimulatorPaymentProperties properties;

    public PgSimulatorPaymentGateway(
        @Qualifier("pgSimulatorRestClient") RestClient restClient,
        PgSimulatorPaymentProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public PaymentResult pay(Long orderId, Money amount) {
        Optional<String> transactionKey = createTransaction(orderId, amount);
        if (transactionKey.isEmpty()) {
            return PaymentResult.failure();
        }

        PgTransactionStatus status = waitForFinalStatus(transactionKey.get());
        return status == PgTransactionStatus.CAPTURED ? PaymentResult.success() : PaymentResult.failure();
    }

    @Override
    public void refund(Long orderId, Money amount) {
        String transactionKey = findRefundableTransaction(orderId);
        try {
            PgEnvelope<PgTransaction> envelope = restClient.post()
                .uri("/api/v1/payments/{transactionKey}/refunds", transactionKey)
                .body(new PgRefundPayload(amount.amount()))
                .retrieve()
                .body(TRANSACTION_TYPE);

            if (!isSuccess(envelope) || envelope.data() == null || envelope.data().status() == null
                || !envelope.data().status().isRefunded()) {
                throw new CoreException(ErrorType.INTERNAL_ERROR,
                    "PG 환불 결과를 확인하지 못했습니다. " + failureMessage(envelope));
            }
        } catch (RestClientException e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 환불 요청에 실패했습니다.");
        }
    }

    private Optional<String> createTransaction(Long orderId, Money amount) {
        PgPaymentPayload payload = new PgPaymentPayload(
            toPgOrderId(orderId),
            properties.getCardType(),
            properties.getCardNo(),
            amount.amount(),
            properties.getCallbackUrl().toString()
        );

        try {
            PgEnvelope<PgTransaction> envelope = restClient.post()
                .uri("/api/v1/payments")
                .body(payload)
                .retrieve()
                .body(TRANSACTION_TYPE);

            if (!isSuccess(envelope) || envelope.data() == null || envelope.data().transactionKey() == null) {
                log.warn("PG simulator payment request failed. orderId={}, message={}",
                    orderId, failureMessage(envelope));
                return Optional.empty();
            }
            return Optional.of(envelope.data().transactionKey());
        } catch (RestClientException e) {
            log.warn("PG simulator payment request failed. orderId={}, message={}", orderId, e.getMessage());
            return Optional.empty();
        }
    }

    private PgTransactionStatus waitForFinalStatus(String transactionKey) {
        long deadline = System.nanoTime() + properties.getCaptureTimeout().toNanos();
        while (true) {
            PgTransactionStatus status = fetchStatus(transactionKey);
            if (status != PgTransactionStatus.PENDING) {
                return status;
            }
            if (System.nanoTime() >= deadline) {
                throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 결과 확인 시간이 초과되었습니다.");
            }
            sleep(properties.getPollInterval());
        }
    }

    private String findRefundableTransaction(Long orderId) {
        try {
            PgEnvelope<PgOrderTransactions> envelope = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/payments")
                    .queryParam("orderId", toPgOrderId(orderId))
                    .build())
                .retrieve()
                .body(ORDER_TYPE);

            if (!isSuccess(envelope) || envelope.data() == null || envelope.data().transactions() == null) {
                throw new CoreException(ErrorType.INTERNAL_ERROR,
                    "PG 결제 내역을 확인하지 못했습니다. " + failureMessage(envelope));
            }
            return envelope.data().transactions().stream()
                .filter(transaction -> transaction.status() != null && transaction.status().isRefundable())
                .map(PgTransaction::transactionKey)
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.INTERNAL_ERROR,
                    "환불 가능한 PG 결제 내역이 없습니다."));
        } catch (RestClientException e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 내역 조회에 실패했습니다.");
        }
    }

    private PgTransactionStatus fetchStatus(String transactionKey) {
        try {
            PgEnvelope<PgTransactionDetail> envelope = restClient.get()
                .uri("/api/v1/payments/{transactionKey}", transactionKey)
                .retrieve()
                .body(DETAIL_TYPE);

            if (!isSuccess(envelope) || envelope.data() == null || envelope.data().status() == null) {
                throw new CoreException(ErrorType.INTERNAL_ERROR,
                    "PG 결제 결과를 확인하지 못했습니다. " + failureMessage(envelope));
            }
            return envelope.data().status();
        } catch (RestClientException e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 결과 조회에 실패했습니다.");
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 결과 확인이 중단되었습니다.");
        }
    }

    private String toPgOrderId(Long orderId) {
        String value = String.valueOf(Objects.requireNonNull(orderId, "orderId must not be null"));
        return value.length() >= 6 ? value : "0".repeat(6 - value.length()) + value;
    }

    private boolean isSuccess(PgEnvelope<?> envelope) {
        return envelope != null && envelope.meta() != null && envelope.meta().result() == PgResult.SUCCESS;
    }

    private String failureMessage(PgEnvelope<?> envelope) {
        if (envelope == null || envelope.meta() == null) {
            return "empty response";
        }
        return envelope.meta().message();
    }

    private record PgPaymentPayload(
        String orderId,
        PgSimulatorPaymentProperties.CardType cardType,
        String cardNo,
        long amount,
        String callbackUrl
    ) {
    }

    private record PgRefundPayload(long amount) {
    }

    private record PgEnvelope<T>(PgMetadata meta, T data) {
    }

    private record PgMetadata(PgResult result, String errorCode, String message) {
    }

    private enum PgResult {
        SUCCESS,
        FAIL
    }

    private record PgTransaction(
        String transactionKey,
        PgTransactionStatus status,
        long refundedAmount,
        long refundableAmount,
        String reason
    ) {
    }

    private record PgOrderTransactions(String orderId, java.util.List<PgTransaction> transactions) {
    }

    private record PgTransactionDetail(
        String transactionKey,
        String orderId,
        PgSimulatorPaymentProperties.CardType cardType,
        String cardNo,
        long amount,
        PgTransactionStatus status,
        String reason
    ) {
    }

    private enum PgTransactionStatus {
        PENDING,
        CAPTURED,
        PARTIALLY_REFUNDED,
        REFUNDED,
        FAILED;

        boolean isRefundable() {
            return this == CAPTURED || this == PARTIALLY_REFUNDED;
        }

        boolean isRefunded() {
            return this == PARTIALLY_REFUNDED || this == REFUNDED;
        }
    }
}
