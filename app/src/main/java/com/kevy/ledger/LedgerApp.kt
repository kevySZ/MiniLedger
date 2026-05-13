package com.kevy.ledger

import android.app.Application
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.ui.common.AppThemeManager

class LedgerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemeManager.initialize(this)
        AppGraph.initialize(this)
        AppGraph.repository.ensureSeedData()
    }
}
