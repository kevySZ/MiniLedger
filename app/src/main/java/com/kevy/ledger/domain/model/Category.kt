package com.kevy.ledger.domain.model

data class Category(
    val id: Long,
    val bookId: Long,
    val name: String,
    val type: CategoryType,
    val colorHex: String,
    val isActive: Boolean
)
