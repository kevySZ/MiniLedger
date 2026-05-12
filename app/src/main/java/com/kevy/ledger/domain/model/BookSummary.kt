package com.kevy.ledger.domain.model

data class BookSummary(
    val incomeCents: Long,
    val expenseCents: Long
) {
    val balanceCents: Long = incomeCents - expenseCents
}
