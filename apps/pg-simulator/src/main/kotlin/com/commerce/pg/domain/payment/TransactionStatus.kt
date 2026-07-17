package com.commerce.pg.domain.payment

enum class TransactionStatus {
    PENDING,
    CAPTURED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    FAILED,
}
