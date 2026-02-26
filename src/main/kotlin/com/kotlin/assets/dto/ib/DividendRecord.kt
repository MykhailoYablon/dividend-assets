package com.kotlin.assets.dto.ib

import java.math.BigDecimal
import java.time.LocalDate

data class DividendRecord(
    val symbol: String,
    val date: LocalDate,
    val amount: BigDecimal,
    val type: String,
    val description: String
)