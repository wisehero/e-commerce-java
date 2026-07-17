package com.commerce.pg.infrastructure.payment

import com.commerce.pg.domain.payment.CardType
import com.commerce.pg.domain.payment.Payment
import com.commerce.pg.domain.payment.PaymentRelay
import com.commerce.pg.domain.payment.TransactionStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PaymentCoreRelay : PaymentRelay {
    companion object {
        private val logger = LoggerFactory.getLogger(PaymentCoreRelay::class.java)
        private val restTemplate = RestTemplate()
    }

    override fun notify(payment: Payment) {
        runCatching {
            restTemplate.postForEntity(payment.callbackUrl, CallbackPayload.from(payment), Any::class.java)
        }.onFailure { e -> logger.error("콜백 호출을 실패했습니다. {}", e.message, e) }
    }

    private data class CallbackPayload(
        val transactionKey: String,
        val orderId: String,
        val cardType: CardType,
        val cardNo: String,
        val amount: Long,
        val refundedAmount: Long,
        val refundableAmount: Long,
        val status: TransactionStatus,
        val reason: String?,
    ) {
        companion object {
            fun from(payment: Payment): CallbackPayload = CallbackPayload(
                transactionKey = payment.transactionKey,
                orderId = payment.orderId,
                cardType = payment.cardType,
                cardNo = payment.cardNo,
                amount = payment.amount,
                refundedAmount = payment.refundedAmount,
                refundableAmount = payment.refundableAmount,
                status = payment.status,
                reason = payment.reason,
            )
        }
    }
}
