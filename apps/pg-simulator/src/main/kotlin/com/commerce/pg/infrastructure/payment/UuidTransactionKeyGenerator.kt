package com.commerce.pg.infrastructure.payment

import com.commerce.pg.domain.payment.TransactionKeyGenerator
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class UuidTransactionKeyGenerator : TransactionKeyGenerator {
    override fun generate(): String {
        val now = LocalDateTime.now()
        val uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6)
        return "${DATETIME_FORMATTER.format(now)}:$KEY_TRANSACTION:$uuid"
    }

    companion object {
        private const val KEY_TRANSACTION = "TR"
        private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
