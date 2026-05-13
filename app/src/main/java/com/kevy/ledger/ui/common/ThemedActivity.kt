package com.kevy.ledger.ui.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class ThemedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(AppThemeManager.currentMode(this).themeRes)
        super.onCreate(savedInstanceState)
    }
}
