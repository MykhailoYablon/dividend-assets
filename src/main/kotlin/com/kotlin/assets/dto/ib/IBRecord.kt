package com.kotlin.assets.dto.ib

import com.opencsv.bean.CsvBindByPosition
import java.math.BigDecimal

open class IBRecord(
    @CsvBindByPosition(position = 3) var date: String = "",
    @CsvBindByPosition(position = 4) var description: String = "",
    @CsvBindByPosition(position = 5) var amount: BigDecimal = BigDecimal.ZERO
)
