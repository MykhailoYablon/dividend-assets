package com.kotlin.assets.dto.green

import com.opencsv.bean.CsvBindByPosition
import java.math.BigDecimal
import java.time.LocalDateTime

data class GreenReturnReport(

    var year: Int = 0,
    @CsvBindByPosition(position = 1)
    var date: LocalDateTime,
    @CsvBindByPosition(position = 5)
    var amount: BigDecimal = BigDecimal.ZERO,

    var exchangeRate: BigDecimal = BigDecimal.ZERO,

    var usdValue: BigDecimal = BigDecimal.ZERO
)