package com.commerce.pg.infrastructure.payment

import com.commerce.pg.domain.payment.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<Payment, String> {
    fun findByUserIdAndTransactionKey(userId: String, transactionKey: String): Payment?
    fun findByUserIdAndOrderId(userId: String, orderId: String): List<Payment>
}
