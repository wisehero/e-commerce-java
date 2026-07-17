package com.commerce.pg.domain.payment

import com.commerce.pg.support.error.CoreException
import com.commerce.pg.support.error.ErrorType
import java.time.LocalDateTime

class Payment private constructor(
    val transactionKey: String,
    val userId: String,
    val orderId: String,
    val cardType: CardType,
    val cardNo: String,
    val amount: Long,
    val callbackUrl: String,
    status: TransactionStatus,
    refundedAmount: Long,
    reason: String?,
    val createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) {
    var status: TransactionStatus = status
        private set

    var refundedAmount: Long = refundedAmount
        private set

    val refundableAmount: Long
        get() = amount - refundedAmount

    var reason: String? = reason
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    fun capture() {
        if (status != TransactionStatus.PENDING) {
            throw CoreException(ErrorType.CONFLICT, "결제 매입은 대기 상태에서만 가능합니다.")
        }
        status = TransactionStatus.CAPTURED
        reason = "결제가 정상적으로 매입되었습니다."
        touch()
    }

    fun invalidCard() {
        if (status != TransactionStatus.PENDING) {
            throw CoreException(ErrorType.CONFLICT, "결제 처리는 대기 상태에서만 가능합니다.")
        }
        status = TransactionStatus.FAILED
        reason = "잘못된 카드입니다. 다른 카드를 선택해주세요."
        touch()
    }

    fun limitExceeded() {
        if (status != TransactionStatus.PENDING) {
            throw CoreException(ErrorType.CONFLICT, "결제 처리는 대기 상태에서만 가능합니다.")
        }
        status = TransactionStatus.FAILED
        reason = "한도초과입니다. 다른 카드를 선택해주세요."
        touch()
    }

    fun refund(amount: Long) {
        if (amount <= 0L) {
            throw CoreException(ErrorType.BAD_REQUEST, "환불 금액은 0보다 커야 합니다.")
        }
        if (status != TransactionStatus.CAPTURED && status != TransactionStatus.PARTIALLY_REFUNDED) {
            throw CoreException(ErrorType.CONFLICT, "매입되었거나 부분 환불된 결제만 환불할 수 있습니다.")
        }
        if (amount > refundableAmount) {
            throw CoreException(ErrorType.BAD_REQUEST, "환불 금액은 환불 가능 금액을 초과할 수 없습니다.")
        }

        refundedAmount += amount
        status = if (refundableAmount == 0L) {
            TransactionStatus.REFUNDED
        } else {
            TransactionStatus.PARTIALLY_REFUNDED
        }
        reason = if (status == TransactionStatus.REFUNDED) {
            "결제가 전액 환불되었습니다."
        } else {
            "결제가 부분 환불되었습니다."
        }
        touch()
    }

    private fun touch() {
        updatedAt = LocalDateTime.now()
    }

    companion object {
        fun create(
            transactionKey: String,
            userId: String,
            orderId: String,
            cardType: CardType,
            cardNo: String,
            amount: Long,
            callbackUrl: String,
        ): Payment {
            if (amount <= 0L) {
                throw CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.")
            }
            val now = LocalDateTime.now()
            return Payment(
                transactionKey = transactionKey,
                userId = userId,
                orderId = orderId,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
                callbackUrl = callbackUrl,
                status = TransactionStatus.PENDING,
                refundedAmount = 0L,
                reason = null,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun rehydrate(
            transactionKey: String,
            userId: String,
            orderId: String,
            cardType: CardType,
            cardNo: String,
            amount: Long,
            callbackUrl: String,
            status: TransactionStatus,
            refundedAmount: Long,
            reason: String?,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
        ): Payment = Payment(
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
    }
}
