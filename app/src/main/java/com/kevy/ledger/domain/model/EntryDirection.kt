package com.kevy.ledger.domain.model

enum class EntryDirection {
    IN,
    OUT,
    NONE;

    companion object {
        fun fromValue(value: String): EntryDirection = entries.firstOrNull { it.name == value } ?: NONE
    }
}
