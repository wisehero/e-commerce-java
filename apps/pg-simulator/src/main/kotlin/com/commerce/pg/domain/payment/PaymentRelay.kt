package com.commerce.pg.domain.payment

interface PaymentRelay {
    fun notify(payment: Payment)
}
