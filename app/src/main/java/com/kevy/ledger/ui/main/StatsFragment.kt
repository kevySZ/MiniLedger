package com.kevy.ledger.ui.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kevy.ledger.R
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.databinding.FragmentStatsBinding
import com.kevy.ledger.databinding.ItemStatBarBinding
import com.kevy.ledger.domain.model.CategoryStat
import com.kevy.ledger.domain.model.CategoryType
import com.kevy.ledger.ui.common.CategoryPalette
import com.kevy.ledger.ui.common.Refreshable
import com.kevy.ledger.ui.stats.CategoryRingChartView
import com.kevy.ledger.util.DateUtils
import com.kevy.ledger.util.MoneyUtils
import java.time.YearMonth
import kotlin.math.roundToInt

class StatsFragment : Fragment(R.layout.fragment_stats), Refreshable {
    private var binding: FragmentStatsBinding? = null
    private val repository get() = AppGraph.repository
    private var currentMonth: YearMonth = YearMonth.now()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStatsBinding.bind(view)

        binding?.buttonPrevMonth?.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            refreshContent()
        }
        binding?.buttonNextMonth?.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            refreshContent()
        }

        refreshContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun refreshContent() {
        val bookId = repository.getSelectedBookId()
        val stats = repository.getStats(bookId, currentMonth)
        val isEmpty = stats.dailyStats.isEmpty() && stats.expenseStats.isEmpty() && stats.incomeStats.isEmpty()

        binding?.apply {
            textMonth.text = DateUtils.formatMonth(currentMonth)
            textStatIncome.text = MoneyUtils.centsToDisplay(stats.summary.incomeCents)
            textStatExpense.text = MoneyUtils.centsToDisplay(stats.summary.expenseCents)
            textStatBalance.text = MoneyUtils.centsToDisplay(stats.summary.balanceCents)
            textEmptyStats.visibility = if (isEmpty) View.VISIBLE else View.GONE
            trendChart.setStats(stats.dailyStats)
            ringExpenseCategories.setSegments(buildSegments(stats.expenseStats, CategoryType.EXPENSE))

            renderStatList(layoutExpenseStats, stats.expenseStats, CategoryType.EXPENSE)
            renderStatList(layoutIncomeStats, stats.incomeStats, CategoryType.INCOME)
        }
    }

    private fun buildSegments(
        stats: List<CategoryStat>,
        categoryType: CategoryType
    ): List<CategoryRingChartView.Segment> {
        return stats.map { item ->
            CategoryRingChartView.Segment(
                ratio = item.percentage,
                color = Color.parseColor(CategoryPalette.colorHexFor(item.name, categoryType))
            )
        }
    }

    private fun renderStatList(
        container: LinearLayout,
        stats: List<CategoryStat>,
        categoryType: CategoryType
    ) {
        container.removeAllViews()
        if (stats.isEmpty()) {
            container.addView(
                TextView(requireContext()).apply {
                    text = getString(R.string.empty_stats)
                    setTextColor(requireContext().getColor(R.color.text_secondary))
                }
            )
            return
        }

        val inflater = LayoutInflater.from(requireContext())
        stats.forEach { item ->
            val color = Color.parseColor(CategoryPalette.colorHexFor(item.name, categoryType))
            val percent = (item.percentage * 100f).roundToInt().coerceIn(0, 100)
            val row = ItemStatBarBinding.inflate(inflater, container, false)

            row.textName.text = item.name
            row.textAmount.text = getString(
                R.string.stats_amount_percent,
                MoneyUtils.centsToDisplay(item.amountCents),
                percent
            )
            row.progressBar.progress = percent
            row.progressBar.progressTintList = ColorStateList.valueOf(color)
            row.progressBar.progressBackgroundTintList =
                ColorStateList.valueOf(requireContext().getColor(R.color.ring_track))

            container.addView(row.root)
        }
    }
}
