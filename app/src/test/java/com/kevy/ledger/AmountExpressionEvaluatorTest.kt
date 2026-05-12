package com.kevy.ledger

import com.kevy.ledger.util.AmountExpressionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AmountExpressionEvaluatorTest {
    @Test
    fun evaluatesBasicExpression() {
        assertEquals(2050L, AmountExpressionEvaluator.evaluateToCents("18+2.5"))
    }

    @Test
    fun respectsMultiplicationPriority() {
        assertEquals(700L, AmountExpressionEvaluator.evaluateToCents("1+2*3"))
    }

    @Test
    fun handlesDivision() {
        assertEquals(2500L, AmountExpressionEvaluator.evaluateToCents("100/4"))
    }

    @Test
    fun throwsForDivideByZero() {
        assertThrows(IllegalArgumentException::class.java) {
            AmountExpressionEvaluator.evaluateToCents("9/0")
        }
    }

    @Test
    fun throwsForInvalidExpression() {
        assertThrows(IllegalArgumentException::class.java) {
            AmountExpressionEvaluator.evaluateToCents("1..2")
        }
    }
}
