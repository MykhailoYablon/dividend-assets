package com.kotlin.assets.dto.green

import java.math.BigDecimal
import java.time.LocalDate

data class SolarReportDto(

    var year: Int = 0,
    var date: LocalDate,
    var amount: BigDecimal = BigDecimal.ZERO,

    var exchangeRate: BigDecimal = BigDecimal.ZERO,

    var usdValue: BigDecimal = BigDecimal.ZERO
)