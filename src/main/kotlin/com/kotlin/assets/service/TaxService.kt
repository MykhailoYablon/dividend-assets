package com.kotlin.assets.service

import com.kotlin.assets.dto.TotalTaxReportDto
import com.kotlin.assets.parser.IBFilesParser
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

@Service
class TaxService(
    val parser: IBFilesParser,
    val exchangeRateService: ExchangeRateService
) {
    fun calculateDividendTax(year: Short, file: MultipartFile, military: Boolean): TotalTaxReportDto {

        val parseIBPositions = parser.parseDividendFile(file)

        val dividends = parseIBPositions.dividends

        val rateCache = dividends.map { it.date }
            .toSet()
            .associateWith { date -> exchangeRateService.getRateForDate(date) }


        return TotalTaxReportDto(
            2025, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, mutableListOf()
        );
    }
}