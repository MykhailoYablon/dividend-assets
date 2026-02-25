package com.kotlin.assets.dto.ib

import com.opencsv.bean.CsvBindByPosition
import java.math.BigDecimal

open class IBTrade(
    @CsvBindByPosition(position = 5) var symbol: String = "",
    @CsvBindByPosition(position = 6) var date: String = "",
    @CsvBindByPosition(position = 12) var basis: BigDecimal = BigDecimal.ZERO,
    @CsvBindByPosition(position = 13) var realizedPL: BigDecimal = BigDecimal.ZERO,
    @CsvBindByPosition(position = 15) var code: String = ""
)
