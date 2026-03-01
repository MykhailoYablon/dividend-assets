package com.kotlin.assets.mapper

import com.kotlin.assets.dto.tax.*
import com.kotlin.assets.entity.tax.DividendRecord
import com.kotlin.assets.entity.tax.StockRecord
import com.kotlin.assets.entity.tax.TotalDividendReport
import com.kotlin.assets.entity.tax.TotalStockReport
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface TaxReportMapper {

    fun toTotalReportDto(
        totalStockReport: TotalStockReport,
        totalDividendReport: TotalDividendReport
    ): TotalTaxReportDto

    @Mapping(source = "records", target = "stockRecords")
    fun toDto(report: TotalStockReport): TotalStockReportDto

    fun toDto(report: StockRecord): StockRecordDto

    @Mapping(source = "records", target = "dividendRecords")
    fun toDto(report: TotalDividendReport): TotalDividendReportDto

    fun toDto(report: DividendRecord): DividendRecordDto
}