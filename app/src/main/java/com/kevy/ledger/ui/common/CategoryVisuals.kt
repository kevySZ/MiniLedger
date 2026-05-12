package com.kevy.ledger.ui.common

import com.kevy.ledger.domain.model.CategoryType
import com.kevy.ledger.domain.model.TransactionType

data class CategoryVisual(
    val icon: String,
    val colorHex: String
)

object CategoryVisuals {
    fun forCategory(name: String?, type: CategoryType?, colorHex: String?): CategoryVisual {
        val resolvedColor = CategoryPalette.colorHexFor(name, type, colorHex)
        val icon = when (name.orEmpty()) {
            "餐饮", "用餐" -> "\uD83C\uDF7D"
            "交通" -> "\uD83D\uDE95"
            "购物" -> "\uD83D\uDED2"
            "住房" -> "\uD83C\uDFE0"
            "医疗" -> "\u2695"
            "娱乐" -> "\uD83C\uDFAC"
            "通讯" -> "\uD83D\uDCF1"
            "人情", "红包", "礼品卡" -> "\uD83C\uDF81"
            "旅行" -> "\u2708"
            "工资" -> "\uD83D\uDCBC"
            "奖金" -> "\uD83C\uDFC6"
            "兼职" -> "\uD83D\uDCDA"
            "理财", "投资" -> "\uD83D\uDCC8"
            "退款", "退税" -> "\u21A9"
            "一般", "其他" -> "\uD83D\uDCC3"
            "报销" -> "\uD83D\uDCB1"
            "利息" -> "\uD83D\uDCB0"
            "还款" -> "\uD83D\uDC5B"
            "借款" -> "\uD83D\uDC8E"
            "回款" -> "\uD83D\uDCB8"
            "结余" -> "\uD83D\uDCB5"
            "实物" -> "\uD83C\uDF81"
            "微信刷卡" -> "\u2705"
            "代金券" -> "\uD83C\uDFAB"
            "手续费" -> "\u2699"
            else -> name?.firstOrNull()?.toString() ?: "\u00B7"
        }
        return CategoryVisual(icon = icon, colorHex = resolvedColor)
    }

    fun forTransaction(categoryName: String?, type: TransactionType): CategoryVisual {
        return when (type) {
            TransactionType.TRANSFER -> CategoryVisual("\u21C4", "#6C91BF")
            TransactionType.BALANCE_ADJUSTMENT -> CategoryVisual("\u25CE", "#E9C46A")
            TransactionType.INCOME -> forCategory(categoryName, CategoryType.INCOME, null)
            TransactionType.EXPENSE -> forCategory(categoryName, CategoryType.EXPENSE, null)
        }
    }
}
