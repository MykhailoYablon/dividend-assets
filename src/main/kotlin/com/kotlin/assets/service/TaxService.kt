package com.kotlin.assets.service

import com.kotlin.assets.dto.TotalTaxReportDto
import com.kotlin.assets.dto.enums.ReportStatus
import com.kotlin.assets.entity.DividendTaxReport
import com.kotlin.assets.entity.TotalTaxReport
import com.kotlin.assets.mapper.TaxReportMapper
import com.kotlin.assets.parser.IBFilesParser
import com.kotlin.assets.repository.TotalTaxReportRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class TaxService(
    val parser: IBFilesParser,
    val exchangeRateService: ExchangeRateService,
    val totalTaxReportRepository: TotalTaxReportRepository,
    val taxReportMapper: TaxReportMapper
) {

    val scale = 2
    val roundingMode: RoundingMode = RoundingMode.HALF_DOWN
    val exportDir = "exports"

    @Transactional
    fun calculateDividendTax(year: Short, file: MultipartFile, isMilitary: Boolean): TotalTaxReportDto {

        val parseIBPositions = parser.parseDividendFile(file)

        val dividends = parseIBPositions.dividends

        val rateCache = dividends.map { it.date }
            .toSet()
            .associateWith { date -> exchangeRateService.getRateForDate(date) }

        var totalAmount = BigDecimal.ZERO
        var totalUaBrutto = BigDecimal.ZERO
        val reports: MutableList<DividendTaxReport> = mutableListOf()

        dividends.forEach { dividend ->
            val dividendAmount = dividend.amount
            val dividendDate = dividend.date
            val exchangeRate = rateCache[dividendDate] ?: BigDecimal.ZERO
            val uaBrutto = exchangeRate.multiply(dividendAmount)
                .setScale(scale, roundingMode)

            totalAmount += dividendAmount
            totalUaBrutto += uaBrutto

            val tax9: BigDecimal = uaBrutto.multiply(BigDecimal.valueOf(0.09))
                .setScale(scale, roundingMode)
            val militaryTax5 = uaBrutto.multiply(BigDecimal.valueOf(0.05))
                .setScale(scale, roundingMode)
            //if you are in military then don't
            val taxSum: BigDecimal = if (isMilitary) tax9 else tax9.add(militaryTax5)

            reports.add(
                DividendTaxReport(
                    symbol = dividend.symbol,
                    date = dividendDate,
                    amount = dividendAmount,
                    nbuRate = exchangeRate,
                    uaBrutto = uaBrutto,
                    tax9 = tax9,
                    militaryTax5 = militaryTax5,
                    taxSum = taxSum
                )
            )
        }

        // Calculate tax on taxReport (not sum of individual taxes)
        var totalTax9 = round(totalUaBrutto.multiply(BigDecimal("0.09")))
        var totalMilitaryTax5 = round(totalUaBrutto.multiply(BigDecimal("0.05")))
        var totalTaxSum = round(totalTax9.add(totalMilitaryTax5))

        val totalTaxReport =
            totalTaxReportRepository.findByYear(year).orElseGet {
                TotalTaxReport(
                    year = year, status = ReportStatus.CALCULATED
                )
            }

        totalTaxReport.apply {
            this.totalUaBrutto = totalUaBrutto
            this.totalTax9 = totalTax9
            this.totalMilitaryTax5 = totalMilitaryTax5
            this.totalTaxSum = totalTaxSum
            this.setTaxReports(reports)
        }

        val taxReport = totalTaxReportRepository.save(totalTaxReport)

        return taxReportMapper.toDto(taxReport)
    }

    private fun round(value: BigDecimal): BigDecimal {
        return value.setScale(2, RoundingMode.HALF_UP)
    }

    fun generateXmlTaxReport(year: Short) {
        val totalTaxReport = totalTaxReportRepository.findByYear(year)
            .orElseThrow { throw ResponseStatusException(HttpStatus.NOT_FOUND) }
    }
}