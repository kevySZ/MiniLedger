package com.kevy.ledger.domain.model

data class Account(
    val id: Long,
    val bookId: Long,
    val name: String,
    val type: String,
    val initialBalanceCents: Long,
    val note: String,
    val isActive: Boolean,
    val currentBalanceCents: Long = initialBalanceCents
)
