package com.commerce.pg.infrastructure.payment

import com.commerce.pg.domain.payment.CardType
import com.commerce.pg.domain.payment.Payment
import com.commerce.pg.domain.payment.TransactionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_user_transaction", columnList = "user_id, transaction_key"),
        Index(name = "idx_user_order", columnList = "user_id, order_id"),
        Index(name = "idx_unique_user_order_transaction", columnList = "user_id, order_id, transaction_key", unique = true),
    ],
)
class PaymentJpaEntity(
    @Id
    @Column(name = "transaction_key", nullable = false, unique = true)
    val transactionKey: String,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "order_id", nullable = false)
    val orderId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    val cardType: CardType,

    @Column(name = "card_no", nullable = false)
    val cardNo: String,

    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Column(name = "callback_url", nullable = false)
    val callbackUrl: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: TransactionStatus,

    @Column(name = "refunded_amount", nullable = false)
    val refundedAmount: Long,

    @Column(name = "reason")
    val reason: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime,
) {
    fun toDomain(): Payment = Payment.rehydrate(
        transactionKey = transactionKey,
        userId = userId,
        orderId = orderId,
        cardType = cardType,
        cardNo = cardNo,
        amount = amount,
        callbackUrl = callbackUrl,
        status = status,
        refundedAmount = refundedAmount,
        reason = reason,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun from(payment: Payment): PaymentJpaEntity = PaymentJpaEntity(
            transactionKey = payment.transactionKey,
            userId = payment.userId,
            orderId = payment.orderId,
            cardType = payment.cardType,
            cardNo = payment.cardNo,
            amount = payment.amount,
            callbackUrl = payment.callbackUrl,
            status = payment.status,
            refundedAmount = payment.refundedAmount,
            reason = payment.reason,
            createdAt = payment.createdAt,
            updatedAt = payment.updatedAt,
        )
    }
}
