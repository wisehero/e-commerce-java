package com.commerce.pg.domain.payment

interface TransactionKeyGenerator {
    fun generate(): String
}
