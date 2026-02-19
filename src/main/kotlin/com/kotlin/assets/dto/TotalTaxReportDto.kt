package com.kotlin.assets.dto

import java.math.BigDecimal

data class TotalTaxReportDto(
    var year: Short,
    var totalAmount: BigDecimal,
    var totalUaBrutto: BigDecimal,
    var totalTax9: BigDecimal,
    var totalMilitaryTax5: BigDecimal,
    var totalTaxSum: BigDecimal,
    var taxReportDtos: MutableList<DividendTaxReportDto>)
