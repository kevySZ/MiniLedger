package com.kevy.ledger.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.kevy.ledger.R
import com.kevy.ledger.ui.common.AppThemeManager

class CategoryRingChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    data class Segment(val ratio: Float, val color: Int)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppThemeManager.resolveColor(context, R.attr.ledgerRingTrack)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppThemeManager.resolveColor(context, R.attr.ledgerTextSecondary)
        textAlign = Paint.Align.CENTER
        textSize = 34f
    }
    private val arcBounds = RectF()
    private var segments: List<Segment> = emptyList()

    fun setSegments(items: List<Segment>) {
        segments = items
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return

        val stroke = size * 0.13f
        trackPaint.strokeWidth = stroke
        segmentPaint.strokeWidth = stroke

        val inset = stroke / 2f + 8f
        arcBounds.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(arcBounds, -90f, 360f, false, trackPaint)

        if (segments.isEmpty()) {
            canvas.drawText(context.getString(R.string.stats_ring_empty), width / 2f, height / 2f + 10f, centerTextPaint)
            return
        }

        var startAngle = -90f
        segments.forEach { segment ->
            val sweep = (segment.ratio.coerceIn(0f, 1f) * 360f).coerceAtLeast(3f)
            segmentPaint.color = segment.color
            canvas.drawArc(arcBounds, startAngle, sweep, false, segmentPaint)
            startAngle += sweep
        }
    }
}
