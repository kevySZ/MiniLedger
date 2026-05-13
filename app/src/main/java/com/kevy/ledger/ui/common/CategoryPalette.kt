package com.kevy.ledger.ui.common

import com.kevy.ledger.domain.model.CategoryType
import kotlin.math.abs

object CategoryPalette {
    private val warmExpenseColors = mapOf(
        "餐饮" to "#8FC9B4",
        "交通" to "#A8C8E8",
        "购物" to "#F1BA95",
        "住房" to "#E8D097",
        "医疗" to "#E5AAA4",
        "娱乐" to "#CDBBE8",
        "通讯" to "#A4B8E3",
        "学习" to "#A7D6C6",
        "旅行" to "#93D3D0",
        "礼物" to "#E6AFC0",
        "宠物" to "#F0CD9B",
        "其他" to "#C9BDB3"
    )

    private val warmIncomeColors = mapOf(
        "工资" to "#7EBEA7",
        "奖金" to "#EFC17E",
        "兼职" to "#B2C8E8",
        "理财" to "#8FB5D8",
        "退款" to "#A9D5B3",
        "其他" to "#C9BDB3"
    )

    private val barbieExpenseColors = mapOf(
        "餐饮" to "#E6A6B8",
        "交通" to "#B7B8F2",
        "购物" to "#F5A38E",
        "住房" to "#E6C06E",
        "医疗" to "#F09AB0",
        "娱乐" to "#D5A9F2",
        "通讯" to "#C4A6E8",
        "学习" to "#D8B7F4",
        "旅行" to "#F2A7C5",
        "礼物" to "#F6B5C9",
        "宠物" to "#F4C28A",
        "其他" to "#D9B7C9"
    )

    private val barbieIncomeColors = mapOf(
        "工资" to "#C59AE2",
        "奖金" to "#F0C36D",
        "兼职" to "#F4A0B6",
        "理财" to "#CFA0F0",
        "退款" to "#E89AB2",
        "其他" to "#D9B7C9"
    )

    private val warmExpenseFallback = warmExpenseColors.values.toList()
    private val warmIncomeFallback = warmIncomeColors.values.toList()
    private val barbieExpenseFallback = barbieExpenseColors.values.toList()
    private val barbieIncomeFallback = barbieIncomeColors.values.toList()

    fun transferColorHex(): String {
        return if (AppThemeManager.currentMode() == AppThemeMode.BARBIE) "#C7A1E8" else "#A8C4E8"
    }

    fun adjustmentColorHex(): String {
        return if (AppThemeManager.currentMode() == AppThemeMode.BARBIE) "#F1C785" else "#E6C98F"
    }

    fun colorHexFor(name: String?, type: CategoryType?, fallback: String? = null): String {
        val normalized = normalizeCategoryName(name)
        val knownColor = paletteFor(type)[normalized]
        if (knownColor != null) return knownColor
        if (!fallback.isNullOrBlank()) return fallback

        val fallbackPalette = when {
            AppThemeManager.currentMode() == AppThemeMode.BARBIE && type == CategoryType.INCOME -> barbieIncomeFallback
            AppThemeManager.currentMode() == AppThemeMode.BARBIE -> barbieExpenseFallback
            type == CategoryType.INCOME -> warmIncomeFallback
            else -> warmExpenseFallback
        }
        return fallbackPalette[abs(normalized.hashCode()) % fallbackPalette.size]
    }

    private fun paletteFor(type: CategoryType?): Map<String, String> {
        return when (AppThemeManager.currentMode()) {
            AppThemeMode.BARBIE -> if (type == CategoryType.INCOME) barbieIncomeColors else barbieExpenseColors
            AppThemeMode.WARM -> if (type == CategoryType.INCOME) warmIncomeColors else warmExpenseColors
        }
    }

    private fun normalizeCategoryName(name: String?): String {
        return when (name.orEmpty()) {
            "用餐" -> "餐饮"
            "人情", "红包", "礼品卡", "实物" -> "礼物"
            "一般" -> "其他"
            "退税" -> "退款"
            "投资", "利息", "回款", "报销" -> "理财"
            "借款", "还款", "手续费", "微信刷卡" -> "其他"
            else -> name.orEmpty()
        }
    }
}
