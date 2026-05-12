package com.kevy.ledger

import android.app.Application
import com.kevy.ledger.app.AppGraph

class LedgerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.initialize(this)
        AppGraph.repository.ensureSeedData()
    }
}
