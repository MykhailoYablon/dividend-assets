package com.kotlin.assets.dto.tax

import com.kotlin.assets.dto.enums.ReportStatus
import java.math.BigDecimal

data class TotalTaxReportDto(
    var totalStockReport: TotalStockReportDto,
    var totalDividendReport: TotalDividendReportDto,
)

data class TotalStockReportDto(
    var year: Short,
    var status: ReportStatus,
    var totalUaBrutto: BigDecimal,
    var totalUaNetto: BigDecimal,
    var totalTax18: BigDecimal,
    var totalMilitaryTax5: BigDecimal,
    var totalTaxSum: BigDecimal,
    var stockRecords: MutableList<StockRecordDto>
)

data class TotalDividendReportDto(
    var year: Short,
    var status: ReportStatus,
    var totalAmount: BigDecimal,
    var totalUaBrutto: BigDecimal,
    var totalTax9: BigDecimal,
    var totalMilitaryTax5: BigDecimal,
    var totalTaxSum: BigDecimal,
    var dividendRecords: MutableList<DividendRecordDto>
)