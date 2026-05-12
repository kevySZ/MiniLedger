package com.kevy.ledger.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年MM月")

    fun today(): LocalDate = LocalDate.now()

    fun parseDate(raw: String): LocalDate = LocalDate.parse(raw, dateFormatter)

    fun formatDate(date: LocalDate): String = date.format(displayFormatter)

    fun formatStorageDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatMonth(month: YearMonth): String = month.format(monthFormatter)

    fun toYearMonth(date: LocalDate): YearMonth = YearMonth.of(date.year, date.month)

    fun displayShortWeek(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
}
