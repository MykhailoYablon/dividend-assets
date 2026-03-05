package com.kotlin.assets.service.impl

import com.kotlin.assets.dto.enums.FileType
import com.kotlin.assets.dto.enums.ReportStatus
import com.kotlin.assets.dto.ib.IBDividendRecord
import com.kotlin.assets.dto.ib.IBStockRecord
import com.kotlin.assets.dto.tax.TotalTaxReportDto
import com.kotlin.assets.entity.tax.DividendRecord
import com.kotlin.assets.entity.tax.StockRecord
import com.kotlin.assets.entity.tax.TotalDividendReport
import com.kotlin.assets.entity.tax.TotalStockReport
import com.kotlin.assets.mapper.TaxReportMapper
import com.kotlin.assets.parser.IBFilesParser
import com.kotlin.assets.repository.TotalDividendReportRepository
import com.kotlin.assets.repository.TotalStockReportRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

@Service
class TaxService(
    private val parser: IBFilesParser,
    private val exchangeRateService: ExchangeRateService,
    private val totalDividendReportRepository: TotalDividendReportRepository,
    private val totalStockReportRepository: TotalStockReportRepository,
    private val taxReportMapper: TaxReportMapper
) {

    val scale = 2
    val roundingMode: RoundingMode = RoundingMode.HALF_DOWN

    @Transactional
    fun getTaxReports(year: Short, userId: Long): TotalTaxReportDto {
        val totalStockReport = totalStockReportRepository.findByYear(year)
            .orElseThrow { throw ResponseStatusException(HttpStatus.NOT_FOUND) }

        val totalDividendReport = totalDividendReportRepository.findByYear(year)
            .orElseThrow { throw ResponseStatusException(HttpStatus.NOT_FOUND) }

        return taxReportMapper.toTotalReportDto(totalStockReport, totalDividendReport)
    }

    @Transactional
    fun getTaxReportsOrNull(year: Short): TotalTaxReportDto? {
        val totalStockReport = totalStockReportRepository.findByYear(year).orElse(null) ?: return null
        val totalDividendReport = totalDividendReportRepository.findByYear(year).orElse(null) ?: return null
        return taxReportMapper.toTotalReportDto(totalStockReport, totalDividendReport)
    }

    @Transactional
    fun calculateTax(
        year: Short,
        file: MultipartFile,
        fileType: FileType,
        isMilitary: Boolean
    ): TotalTaxReportDto {
        val ibReport = when (fileType) {
            FileType.CSV -> parser.parseIbCsv(file)
            FileType.XML -> parser.parseIbXml(file)
            FileType.XLSX -> throw IllegalArgumentException("XLSX not supported yet")
        }

        val trades = ibReport.first
        val dividends = ibReport.second

        val allDates = (dividends.map { it.date } +
                trades.values.flatten()
                    .map { it.tradeDate })
            .toSet()

        val rateCache = allDates.associateWith { date ->
            exchangeRateService.getRateForDate(date)
        }

        // Calculate Stock Tax
        val totalStockReport = calculateStockTaxes(year, trades, rateCache)
        // Save Stock Tax
        // Calculate Dividend Tax
        val totalDividendReport = calculateDividendTaxes(dividends, rateCache, isMilitary, year)

        return taxReportMapper.toTotalReportDto(totalStockReport, totalDividendReport)
    }

    private fun calculateDividendTaxes(
        dividends: List<IBDividendRecord>,
        rateCache: Map<LocalDate, BigDecimal>,
        isMilitary: Boolean,
        year: Short
    ): TotalDividendReport {
        var totalAmount = BigDecimal.ZERO
        var totalUaBrutto = BigDecimal.ZERO
        val reports: MutableList<DividendRecord> = mutableListOf()

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
                DividendRecord(
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
        val totalTax9 = round(totalUaBrutto.multiply(BigDecimal("0.09")))
        val totalMilitaryTax5 = round(totalUaBrutto.multiply(BigDecimal("0.05")))
        val totalTaxSum = round(totalTax9.add(totalMilitaryTax5))

        val totalDividendReport =
            totalDividendReportRepository.findByYear(year).orElseGet {
                TotalDividendReport(
                    year = year, status = ReportStatus.CALCULATED
                )
            }

        totalDividendReport.apply {
            this.totalAmount = totalAmount
            this.totalUaBrutto = totalUaBrutto
            this.totalTax9 = totalTax9
            this.totalMilitaryTax5 = totalMilitaryTax5
            this.totalTaxSum = totalTaxSum
            this.setTaxRecords(reports)
        }

        val taxReport = totalDividendReportRepository.save(totalDividendReport)
        return taxReport
    }

    private fun calculateStockTaxes(
        year: Short,
        trades: Map<String, List<IBStockRecord>>,
        rateCache: Map<LocalDate, BigDecimal>
    ): TotalStockReport {

        var totalBuy = BigDecimal.ZERO
        var totalSell = BigDecimal.ZERO
        var totalBrutto = BigDecimal.ZERO
        var totalTax18 = BigDecimal.ZERO
        var totalMilitaryTax5 = BigDecimal.ZERO

        val reports = trades.flatMap { (symbol, records) ->
            val buyQueue = ArrayDeque(
                records
                    .filter { it.buySell == "BUY" }
                    .sortedBy { it.tradeDate }
                    .flatMap { buy ->
                        val rate = rateCache[buy.tradeDate] ?: BigDecimal.ONE
                        List(buy.quantity) {
                            Triple(buy.tradeDate, round(buy.costBasis / BigDecimal(buy.quantity) * rate), rate)
                        }
                    }
            )

            records
                .filter { it.buySell == "SELL" }
                .sortedBy { it.tradeDate }
                .map { sell ->
                    val sellExchangeRate = rateCache[sell.tradeDate] ?: BigDecimal.ZERO

                    val buyEntries = (0 until sell.quantity).map {
                        buyQueue.removeFirst() ?: error("Not enough BUY trades in queue for symbol $symbol")
                    }

                    val totalBuyPriceUAH = round(buyEntries.sumOf { (_, costBasisUah, _) -> costBasisUah })


                    // For reporting purposes - take from first and last buy if spread across multiple
                    val buyDates = buyEntries.map { (buyDate, _, _) -> buyDate }
                    val buyRates = buyEntries.map { (_, _, buyRate) -> buyRate }

                    val ibCommission = round(sell.ibCommission.abs().multiply(sellExchangeRate))
                    val sellPriceUah = round(sellExchangeRate.multiply(sell.tradePrice))
                        .minus(ibCommission)

                    val netProfitUah = round(sellPriceUah - totalBuyPriceUAH)
                    val tax18 = round(netProfitUah.multiply(BigDecimal("0.18")))
                    val militaryTax5 = round(netProfitUah.multiply(BigDecimal("0.05")))

                    totalBuy += totalBuyPriceUAH
                    totalSell += sellPriceUah
                    totalBrutto += netProfitUah
                    totalTax18 += tax18
                    totalMilitaryTax5 += militaryTax5

                    StockRecord(
                        symbol = symbol,
                        sellQuantity = sell.quantity,
                        buyPriceUah = totalBuyPriceUAH,
                        sellPriceUah = sellPriceUah,
                        originalBuyPriceUsd = sell.tradePrice,
                        originalPL = sell.fifoPnlRealized,
                        ibCommission = ibCommission,
                        buyDate = buyDates.first(),
                        sellDate = sell.tradeDate,
                        buyExchangeRate = buyRates.first(),
                        sellExchangeRate = sellExchangeRate,
                        netProfitUah = netProfitUah,
                        tax18 = tax18,
                        militaryTax5 = militaryTax5
                    )
                }
        }

        val totalStockReport =
            totalStockReportRepository.findByYear(year).orElseGet {
                TotalStockReport(
                    year = year, status = ReportStatus.CALCULATED
                )
            }
        val totalTaxSum = totalTax18 + totalMilitaryTax5
        totalStockReport.apply {
            this.totalBuy = totalBuy
            this.totalSell = totalSell
            this.totalUaBrutto = totalBrutto
            this.totalUaNetto = totalBrutto.minus(totalTaxSum)
            this.totalTax18 = totalTax18
            this.totalMilitaryTax5 = totalMilitaryTax5
            this.totalTaxSum = totalTaxSum
            this.setTaxRecords(reports)
        }

        return totalStockReportRepository.save(totalStockReport)
    }

    private fun round(value: BigDecimal): BigDecimal {
        return value.setScale(2, RoundingMode.HALF_UP)
    }

}