package com.kevy.ledger.domain.model

data class StatsSnapshot(
    val summary: BookSummary,
    val expenseStats: List<CategoryStat>,
    val incomeStats: List<CategoryStat>,
    val dailyStats: List<DailyStat>
)
