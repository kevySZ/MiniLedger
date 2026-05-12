package com.kevy.ledger.domain.model

import java.time.LocalDate

data class LedgerTransaction(
    val id: Long,
    val bookId: Long,
    val type: TransactionType,
    val direction: EntryDirection,
    val categoryId: Long?,
    val categoryName: String?,
    val accountId: Long?,
    val accountName: String?,
    val transferAccountId: Long?,
    val transferAccountName: String?,
    val amountCents: Long,
    val note: String,
    val eventDate: LocalDate
)
