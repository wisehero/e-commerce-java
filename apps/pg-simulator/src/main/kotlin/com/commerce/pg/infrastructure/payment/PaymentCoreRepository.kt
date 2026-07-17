package com.commerce.pg.infrastructure.payment

import com.commerce.pg.domain.payment.Payment
import com.commerce.pg.domain.payment.PaymentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Component
class PaymentCoreRepository(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    @Transactional
    override fun save(payment: Payment): Payment {
        return paymentJpaRepository.save(PaymentJpaEntity.from(payment)).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByTransactionKey(transactionKey: String): Payment? {
        return paymentJpaRepository.findById(transactionKey).getOrNull()?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByTransactionKey(userId: String, transactionKey: String): Payment? {
        return paymentJpaRepository.findByUserIdAndTransactionKey(userId, transactionKey)?.toDomain()
    }

    override fun findByOrderId(userId: String, orderId: String): List<Payment> {
        return paymentJpaRepository.findByUserIdAndOrderId(userId, orderId)
            .sortedByDescending { it.updatedAt }
            .map { it.toDomain() }
    }
}
