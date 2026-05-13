package com.kevy.ledger.ui.common

import com.kevy.ledger.domain.model.CategoryType
import kotlin.math.abs

object CategoryPalette {
    const val transferColorHex = "#A8C4E8"
    const val adjustmentColorHex = "#E6C98F"

    private val namedColors = mapOf(
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
        "其他" to "#C9BDB3",
        "工资" to "#7EBEA7",
        "奖金" to "#EFC17E",
        "兼职" to "#B2C8E8",
        "理财" to "#8FB5D8",
        "退款" to "#A9D5B3"
    )

    private val expenseFallback = listOf(
        "#8FC9B4",
        "#A8C8E8",
        "#F1BA95",
        "#E8D097",
        "#E5AAA4",
        "#CDBBE8",
        "#A4B8E3",
        "#A7D6C6",
        "#93D3D0",
        "#E6AFC0",
        "#F0CD9B",
        "#C9BDB3"
    )

    private val incomeFallback = listOf(
        "#7EBEA7",
        "#EFC17E",
        "#B2C8E8",
        "#8FB5D8",
        "#A9D5B3",
        "#C9BDB3"
    )

    fun colorHexFor(name: String?, type: CategoryType?, fallback: String? = null): String {
        val normalized = normalizeCategoryName(name)
        return namedColors[normalized]
            ?: fallback
            ?: when (type) {
                CategoryType.INCOME -> incomeFallback[abs(normalized.hashCode()) % incomeFallback.size]
                else -> expenseFallback[abs(normalized.hashCode()) % expenseFallback.size]
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
