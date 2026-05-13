package com.kevy.ledger.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.kevy.ledger.R
import com.kevy.ledger.domain.model.DailyStat
import kotlin.math.max

class DailyTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.expense)
    }
    private val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.income)
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.divider)
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 24f
    }
    private var stats: List<DailyStat> = emptyList()

    fun setStats(data: List<DailyStat>) {
        stats = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val contentHeight = height - paddingBottom - 24f
        val baseline = contentHeight
        canvas.drawLine(0f, baseline, width.toFloat(), baseline, axisPaint)
        if (stats.isEmpty()) return
        val maxValue = max(1L, stats.maxOf { max(it.incomeCents, it.expenseCents) })
        val barGroupWidth = width.toFloat() / stats.size.toFloat()
        stats.forEachIndexed { index, stat ->
            val startX = index * barGroupWidth
            val expenseHeight = (stat.expenseCents.toFloat() / maxValue.toFloat()) * (contentHeight - 32f)
            val incomeHeight = (stat.incomeCents.toFloat() / maxValue.toFloat()) * (contentHeight - 32f)
            val barWidth = barGroupWidth / 3f
            canvas.drawRect(startX + 8f, baseline - expenseHeight, startX + 8f + barWidth, baseline, expensePaint)
            canvas.drawRect(startX + 16f + barWidth, baseline - incomeHeight, startX + 16f + barWidth * 2f, baseline, incomePaint)
            canvas.drawText(stat.dayOfMonth.toString(), startX + 6f, height.toFloat() - 4f, labelPaint)
        }
    }
}
