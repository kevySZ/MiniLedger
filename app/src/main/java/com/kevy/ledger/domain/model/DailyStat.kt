package com.kevy.ledger.domain.model

data class DailyStat(
    val dayOfMonth: Int,
    val incomeCents: Long,
    val expenseCents: Long
)
