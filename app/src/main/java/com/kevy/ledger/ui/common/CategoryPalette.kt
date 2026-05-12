package com.kevy.ledger.ui.common

import com.kevy.ledger.domain.model.CategoryType
import kotlin.math.abs

object CategoryPalette {
    private val namedColors = mapOf(
        "餐饮" to "#7BC8A4",
        "用餐" to "#7BC8A4",
        "交通" to "#78B7FF",
        "购物" to "#FF9F6E",
        "住房" to "#E9C46A",
        "医疗" to "#F28482",
        "娱乐" to "#C084FC",
        "通讯" to "#5C7AEA",
        "人情" to "#F78FB3",
        "旅行" to "#4ECDC4",
        "工资" to "#2A9D8F",
        "奖金" to "#F4A261",
        "兼职" to "#6C91BF",
        "理财" to "#4D96FF",
        "退款" to "#58C27D",
        "退税" to "#58C27D",
        "一般" to "#FFB703",
        "报销" to "#FB8500",
        "红包" to "#D90429",
        "利息" to "#CC5DE8",
        "还款" to "#C77DFF",
        "借款" to "#9B5DE5",
        "回款" to "#7A3E65",
        "礼品卡" to "#C97B36",
        "投资" to "#F07167",
        "结余" to "#D17B9F",
        "实物" to "#E63946",
        "微信刷卡" to "#2B9348",
        "代金券" to "#A68A64",
        "手续费" to "#F4A7C1",
        "其他" to "#8D99AE"
    )

    private val expenseFallback = listOf(
        "#7BC8A4",
        "#78B7FF",
        "#FF9F6E",
        "#E9C46A",
        "#F28482",
        "#C084FC",
        "#5C7AEA"
    )
    private val incomeFallback = listOf(
        "#2A9D8F",
        "#58C27D",
        "#4D96FF",
        "#F4A261",
        "#9B5DE5",
        "#F78FB3"
    )

    fun colorHexFor(name: String?, type: CategoryType?, fallback: String? = null): String {
        val normalized = name.orEmpty()
        return namedColors[normalized]
            ?: fallback
            ?: when (type) {
                CategoryType.INCOME -> incomeFallback[abs(normalized.hashCode()) % incomeFallback.size]
                else -> expenseFallback[abs(normalized.hashCode()) % expenseFallback.size]
            }
    }
}
