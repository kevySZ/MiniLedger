package com.kevy.ledger.app

import android.content.Context
import com.kevy.ledger.data.db.LedgerDatabaseHelper
import com.kevy.ledger.data.repository.LedgerRepository

object AppGraph {
    private lateinit var databaseHelper: LedgerDatabaseHelper
    lateinit var repository: LedgerRepository
        private set

    fun initialize(context: Context) {
        if (::repository.isInitialized) return
        databaseHelper = LedgerDatabaseHelper(context.applicationContext)
        repository = LedgerRepository(context.applicationContext, databaseHelper)
    }
}
