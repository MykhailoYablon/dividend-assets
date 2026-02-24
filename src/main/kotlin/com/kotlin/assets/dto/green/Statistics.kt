package com.kotlin.assets.dto.green

import java.math.BigDecimal
import java.time.Month

data class Statistics(
    val grandTotal: BigDecimal,
    val grandUsdTotal: BigDecimal,
    val byYear: List<YearSummary>
)

data class MonthSummary(
    val month: Month,
    val total: BigDecimal,
    val usdTotal: BigDecimal,
    val count: Int
)

data class YearSummary(
    val year: Int,
    val total: BigDecimal,
    val usdTotal: BigDecimal,
    val count: Int,
    val byMonth: List<MonthSummary>
)
