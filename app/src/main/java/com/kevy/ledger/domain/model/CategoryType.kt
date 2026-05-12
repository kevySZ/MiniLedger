package com.kevy.ledger.domain.model

enum class CategoryType {
    EXPENSE,
    INCOME;

    companion object {
        fun fromValue(value: String): CategoryType = entries.firstOrNull { it.name == value } ?: EXPENSE
    }
}
