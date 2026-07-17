package com.commerce.pg.domain.payment

object PaymentEvent {
    data class PaymentCreated(
        val transactionKey: String,
    ) {
        companion object {
            fun from(payment: Payment): PaymentCreated = PaymentCreated(transactionKey = payment.transactionKey)
        }
    }

    data class PaymentStatusChanged(val transactionKey: String) {
        companion object {
            fun from(payment: Payment): PaymentStatusChanged = PaymentStatusChanged(payment.transactionKey)
        }
    }
}
