package com.kevy.ledger.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object MoneyUtils {
    private val decimalFormat = DecimalFormat("#,##0.00")

    fun centsToDisplay(cents: Long): String {
        val amount = BigDecimal.valueOf(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return decimalFormat.format(amount)
    }

    fun centsToPlain(cents: Long): String {
        val amount = BigDecimal.valueOf(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return decimalFormat.format(amount)
    }

    fun decimalToCents(amount: BigDecimal): Long {
        return amount.setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal(100)).longValueExact()
    }
}
