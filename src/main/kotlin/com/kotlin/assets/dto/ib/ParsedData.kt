package com.kotlin.assets.dto.ib

import java.math.BigDecimal
import java.time.LocalDate

data class ParsedData(
    var dividends: MutableList<IbData>,
    var withholdingTax: MutableList<IbData>
)

data class IbData(var symbol: String, var date: LocalDate, var amount: BigDecimal)

