package com.kevy.ledger.domain.model

data class Book(
    val id: Long,
    val name: String,
    val colorHex: String,
    val note: String,
    val isDefault: Boolean,
    val isArchived: Boolean
)
