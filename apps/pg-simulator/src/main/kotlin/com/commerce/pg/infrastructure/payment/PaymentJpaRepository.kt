package com.commerce.pg.infrastructure.payment

import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<PaymentJpaEntity, String> {
    fun findByUserIdAndTransactionKey(userId: String, transactionKey: String): PaymentJpaEntity?
    fun findByUserIdAndOrderId(userId: String, orderId: String): List<PaymentJpaEntity>
}
