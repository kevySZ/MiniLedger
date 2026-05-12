package com.kevy.ledger.util

import java.math.BigDecimal
import java.math.RoundingMode

object AmountExpressionEvaluator {
    fun evaluate(expression: String): BigDecimal {
        val sanitized = expression.replace(" ", "")
        require(sanitized.isNotEmpty()) { "金额不能为空" }
        val parser = Parser(sanitized)
        val result = parser.parseExpression()
        require(parser.isFinished()) { "表达式格式不正确" }
        return result.setScale(2, RoundingMode.HALF_UP)
    }

    fun evaluateToCents(expression: String): Long = MoneyUtils.decimalToCents(evaluate(expression))

    private class Parser(private val input: String) {
        private var index = 0

        fun isFinished(): Boolean = index >= input.length

        fun parseExpression(): BigDecimal {
            var value = parseTerm()
            while (index < input.length) {
                when (val operator = input[index]) {
                    '+' -> {
                        index++
                        value = value.add(parseTerm())
                    }

                    '-' -> {
                        index++
                        value = value.subtract(parseTerm())
                    }

                    else -> return value
                }
            }
            return value
        }

        private fun parseTerm(): BigDecimal {
            var value = parseFactor()
            while (index < input.length) {
                when (val operator = input[index]) {
                    '*' -> {
                        index++
                        value = value.multiply(parseFactor())
                    }

                    '/' -> {
                        index++
                        val divisor = parseFactor()
                        require(divisor.compareTo(BigDecimal.ZERO) != 0) { "不能除以 0" }
                        value = value.divide(divisor, 8, RoundingMode.HALF_UP)
                    }

                    else -> return value
                }
            }
            return value
        }

        private fun parseFactor(): BigDecimal {
            require(index < input.length) { "表达式不完整" }
            val start = index
            var dotSeen = false
            while (index < input.length) {
                val char = input[index]
                if (char == '.') {
                    require(!dotSeen) { "表达式格式不正确" }
                    dotSeen = true
                } else if (!char.isDigit()) {
                    break
                }
                index++
            }
            require(start != index) { "表达式格式不正确" }
            return input.substring(start, index).toBigDecimal()
        }
    }
}
