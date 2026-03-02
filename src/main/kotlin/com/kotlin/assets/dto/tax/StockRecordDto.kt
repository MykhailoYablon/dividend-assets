package com.kotlin.assets.dto.tax

import java.math.BigDecimal
import java.time.LocalDate

data class StockRecordDto(
    var symbol: String,
    var sellQuantity: Int,
    var buyPriceUah: BigDecimal,
    var sellPriceUah: BigDecimal,
    var originalBuyPriceUsd: BigDecimal,
    var originalPL: BigDecimal,
    var ibCommission: BigDecimal,
    var buyDate: LocalDate,
    var sellDate: LocalDate,
    var buyExchangeRate: BigDecimal,
    var sellExchangeRate: BigDecimal,
    var netProfitUah: BigDecimal,
    var tax18: BigDecimal,
    var militaryTax5: BigDecimal
)
