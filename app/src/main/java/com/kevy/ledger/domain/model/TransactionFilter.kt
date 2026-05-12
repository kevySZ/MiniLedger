package com.kevy.ledger.domain.model

import java.time.LocalDate

data class TransactionFilter(
    val bookId: Long,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val keyword: String? = null,
    val minAmountCents: Long? = null,
    val maxAmountCents: Long? = null
)
