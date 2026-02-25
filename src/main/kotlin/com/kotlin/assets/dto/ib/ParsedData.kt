package com.kotlin.assets.dto.ib

import java.math.BigDecimal
import java.time.LocalDate

data class ParsedData(
    var dividends: MutableList<IbData>,
    var withholdingTax: MutableList<IbData>,
    var trades: MutableList<TradeData>
)

data class IbData(var symbol: String, var date: LocalDate, var amount: BigDecimal)

data class TradeData(
    var symbol: String,
    var date: LocalDate,
    var basis: BigDecimal,
    var realizedPL: BigDecimal,
    var code: String
)

