package com.kevy.ledger.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    BALANCE_ADJUSTMENT;

    companion object {
        fun fromValue(value: String): TransactionType = entries.firstOrNull { it.name == value } ?: EXPENSE
    }
}
