package com.kotlin.assets.mapper

import com.kotlin.assets.dto.tax.DividendTaxReportDto
import com.kotlin.assets.dto.tax.TotalTaxReportDto
import com.kotlin.assets.entity.DividendTaxReport
import com.kotlin.assets.entity.TotalTaxReport
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface TaxReportMapper {

    @Mapping(source = "reports", target = "taxReportDtos")
    fun toDto(report: TotalTaxReport): TotalTaxReportDto

    fun toDto(report: DividendTaxReport): DividendTaxReportDto
}