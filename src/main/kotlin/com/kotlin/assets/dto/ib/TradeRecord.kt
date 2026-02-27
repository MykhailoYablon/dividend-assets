package com.kotlin.assets.dto.ib

import java.math.BigDecimal
import java.time.LocalDate

data class TradeRecord(
    val symbol: String,
    val tradeDate: LocalDate,
    val quantity: Int,
    val tradePrice: BigDecimal,
    val costBasis: BigDecimal,
    val ibCommission: BigDecimal,
    val fifoPnlRealized: BigDecimal,
    val buySell: String,
)