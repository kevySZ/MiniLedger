package com.kevy.ledger.ui.common

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.kevy.ledger.R

enum class AppThemeMode(
    val storageValue: String,
    val themeRes: Int
) {
    WARM("warm", R.style.Theme_Ledger_Warm),
    BARBIE("barbie", R.style.Theme_Ledger_Barbie);

    companion object {
        fun fromStorage(value: String?): AppThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: WARM
        }
    }
}

object AppThemeManager {
    private const val PREFS_NAME = "ledger_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun currentMode(context: Context = appContext): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppThemeMode.fromStorage(prefs.getString(KEY_THEME_MODE, AppThemeMode.WARM.storageValue))
    }

    fun setMode(context: Context = appContext, mode: AppThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.storageValue)
            .apply()
    }

    @ColorInt
    fun resolveColor(context: Context, @AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
}
