package com.kotlin.assets.dto

import java.math.BigDecimal
import java.time.LocalDate

data class DividendTaxReportDto(
    var symbol: String,
    var date: LocalDate,
    var amount: BigDecimal,
    var nbuRate: BigDecimal,
    var uaBrutto: BigDecimal,
    var tax9: BigDecimal,
    var militaryTax5: BigDecimal,
    var taxSum: BigDecimal
)
