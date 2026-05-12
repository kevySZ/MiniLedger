package com.kevy.ledger.domain.model

import java.time.LocalDate

data class TransactionInput(
    val id: Long? = null,
    val bookId: Long,
    val type: TransactionType,
    val direction: EntryDirection = EntryDirection.NONE,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val transferAccountId: Long? = null,
    val amountCents: Long,
    val note: String,
    val eventDate: LocalDate
)
