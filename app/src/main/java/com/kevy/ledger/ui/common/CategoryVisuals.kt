package com.kevy.ledger.ui.common

import com.kevy.ledger.R
import com.kevy.ledger.domain.model.CategoryType
import com.kevy.ledger.domain.model.TransactionType

data class CategoryVisual(
    val iconRes: Int,
    val colorHex: String
)

object CategoryVisuals {
    fun forCategory(name: String?, type: CategoryType?, colorHex: String?): CategoryVisual {
        val normalized = normalizeCategoryName(name)
        val resolvedType = type ?: CategoryType.EXPENSE
        val resolvedColor = CategoryPalette.colorHexFor(normalized, resolvedType, colorHex)
        val iconRes = when (normalized) {
            "餐饮" -> R.drawable.ic_category_food
            "交通" -> R.drawable.ic_category_transport
            "购物" -> R.drawable.ic_category_shopping
            "住房" -> R.drawable.ic_category_home
            "医疗" -> R.drawable.ic_category_medical
            "娱乐" -> R.drawable.ic_category_entertainment
            "通讯" -> R.drawable.ic_category_phone
            "学习" -> R.drawable.ic_category_study
            "旅行" -> R.drawable.ic_category_travel
            "礼物" -> R.drawable.ic_category_gift
            "宠物" -> R.drawable.ic_category_pet
            "工资" -> R.drawable.ic_category_salary
            "奖金" -> R.drawable.ic_category_bonus
            "兼职" -> R.drawable.ic_category_part_time
            "理财" -> R.drawable.ic_category_finance
            "退款" -> R.drawable.ic_category_refund
            else -> if (resolvedType == CategoryType.INCOME) {
                R.drawable.ic_category_income_other
            } else {
                R.drawable.ic_category_other
            }
        }
        return CategoryVisual(iconRes = iconRes, colorHex = resolvedColor)
    }

    fun forTransaction(categoryName: String?, type: TransactionType): CategoryVisual {
        return when (type) {
            TransactionType.TRANSFER -> CategoryVisual(
                iconRes = R.drawable.ic_category_transfer,
                colorHex = CategoryPalette.transferColorHex()
            )

            TransactionType.BALANCE_ADJUSTMENT -> CategoryVisual(
                iconRes = R.drawable.ic_category_adjustment,
                colorHex = CategoryPalette.adjustmentColorHex()
            )

            TransactionType.INCOME -> forCategory(categoryName, CategoryType.INCOME, null)
            TransactionType.EXPENSE -> forCategory(categoryName, CategoryType.EXPENSE, null)
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
