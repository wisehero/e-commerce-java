package com.commerce.pg.domain.payment

import com.commerce.pg.application.payment.TransactionInfo

interface PaymentRelay {
    fun notify(callbackUrl: String, transactionInfo: TransactionInfo)
}
