package com.kotlin.assets.service.impl

import com.kotlin.assets.dto.enums.FileType
import com.kotlin.assets.dto.enums.ReportStatus
import com.kotlin.assets.dto.ib.IBDividendRecord
import com.kotlin.assets.dto.ib.IBStockRecord
import com.kotlin.assets.dto.tax.TotalTaxReportDto
import com.kotlin.assets.entity.tax.TotalDividendReport
import com.kotlin.assets.entity.tax.TotalStockReport
import com.kotlin.assets.mapper.TaxReportMapper
import com.kotlin.assets.parser.IBFilesParser
import com.kotlin.assets.repository.TotalDividendReportRepository
import com.kotlin.assets.repository.TotalStockReportRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import kotlin.test.Ignore

class TaxServiceTest {

    private val parser: IBFilesParser = mock()
    private val exchangeRateService: ExchangeRateService = mock()
    private val totalDividendReportRepository: TotalDividendReportRepository = mock()
    private val totalStockReportRepository: TotalStockReportRepository = mock()
    private val taxReportMapper: TaxReportMapper = mock()

    private val taxService = TaxService(
        parser,
        exchangeRateService,
        totalDividendReportRepository,
        totalStockReportRepository,
        taxReportMapper
    )

    private val year: Short = 2024
    private val userId: Long = 1L

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun csvFile() = MockMultipartFile("file", "trades.csv", "text/csv", ByteArray(0))

    private fun stockRecord(
        symbol: String = "AAPL",
        buySell: String = "BUY",
        quantity: Int = 10,
        tradePrice: BigDecimal = BigDecimal("150.00"),
        costBasis: BigDecimal = BigDecimal("-1500.00"),
        ibCommission: BigDecimal = BigDecimal("-1.00"),
        fifoPnlRealized: BigDecimal = BigDecimal.ZERO,
        tradeDate: LocalDate = LocalDate.of(2024, 1, 1)
    ) = IBStockRecord(
        symbol = symbol,
        tradeDate = tradeDate,
        quantity = quantity,
        tradePrice = tradePrice,
        costBasis = costBasis,
        ibCommission = ibCommission,
        fifoPnlRealized = fifoPnlRealized,
        buySell = buySell
    )

    private fun dividendRecord(
        symbol: String = "AAPL",
        amount: BigDecimal = BigDecimal("0.92"),
        date: LocalDate = LocalDate.of(2024, 3, 15)
    ) = IBDividendRecord(
        symbol = symbol,
        date = date,
        amount = amount,
        type = "Dividends",
        description = "$symbol Cash Dividend"
    )

    private fun mockEmptyRepos() {
        whenever(totalStockReportRepository.findByYear(year)).thenReturn(Optional.empty())
        whenever(totalDividendReportRepository.findByYear(year)).thenReturn(Optional.empty())
        whenever(totalStockReportRepository.save(any<TotalStockReport>())).thenAnswer { it.arguments[0] }
        whenever(totalDividendReportRepository.save(any<TotalDividendReport>())).thenAnswer { it.arguments[0] }
    }

    // ─── getTaxReports ─────────────────────────────────────────────────────────

    @Test
    fun `getTaxReports - returns mapped DTO when both reports exist`() {
        val stockReport = TotalStockReport(year = year, status = ReportStatus.CALCULATED)
        val dividendReport = TotalDividendReport(year = year, status = ReportStatus.CALCULATED)
        val expectedDto = TotalTaxReportDto()

        whenever(totalStockReportRepository.findByYear(year)).thenReturn(Optional.of(stockReport))
        whenever(totalDividendReportRepository.findByYear(year)).thenReturn(Optional.of(dividendReport))
        whenever(taxReportMapper.toTotalReportDto(stockReport, dividendReport)).thenReturn(expectedDto)

        val result = taxService.getTaxReports(year)

        assertEquals(expectedDto, result)
    }

    // ─── calculateTax – file routing ───────────────────────────────────────────

    @Test
    fun `calculateTax - routes CSV file type to parseIbCsv`() {
        val file = csvFile()
        val emptyResult: Pair<Map<String, List<IBStockRecord>>, List<IBDividendRecord>> =
            Pair(emptyMap(), emptyList())

        whenever(parser.parseIbCsv(file)).thenReturn(emptyResult)
        mockEmptyRepos()
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, file, FileType.CSV, isMilitary = false)

        verify(parser).parseIbCsv(file)
        verify(parser, never()).parseIbXml(any())
    }

    @Test
    fun `calculateTax - routes XML file type to parseIbXml`() {
        val file = csvFile()
        val emptyResult: Pair<Map<String, List<IBStockRecord>>, List<IBDividendRecord>> =
            Pair(emptyMap(), emptyList())

        whenever(parser.parseIbXml(file)).thenReturn(emptyResult)
        mockEmptyRepos()
        whenever(taxReportMapper.toTotalReportDto(any(), any()))
            .thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, file, FileType.XML, isMilitary = false)

        verify(parser).parseIbXml(file)
        verify(parser, never()).parseIbCsv(any())
    }

    @Test
    fun `calculateTax - throws IllegalArgumentException for XLSX file type`() {
        assertThrows<IllegalArgumentException> {
            taxService.calculateTax(year, csvFile(), FileType.XLSX, isMilitary = false)
        }
    }

    // ─── calculateTax – exchange rate caching ──────────────────────────────────

    @Test
    fun `calculateTax - fetches exchange rate for each unique date exactly once`() {
        val buyDate = LocalDate.of(2024, 1, 1)
        val sellDate = LocalDate.of(2024, 2, 1)
        val dividendDate = LocalDate.of(2024, 3, 15)

        val trades = mapOf(
            "AAPL" to listOf(
                stockRecord(buySell = "BUY", tradeDate = buyDate),
                stockRecord(buySell = "SELL", tradeDate = sellDate, costBasis = BigDecimal("1600.00"))
            )
        )
        val dividends = listOf(dividendRecord(date = dividendDate))

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(trades, dividends))
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(BigDecimal("40.00"))
        mockEmptyRepos()
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        // Each unique date fetched exactly once
        verify(exchangeRateService, times(1)).getRateForDate(buyDate)
        verify(exchangeRateService, times(1)).getRateForDate(sellDate)
        verify(exchangeRateService, times(1)).getRateForDate(dividendDate)
        verifyNoMoreInteractions(exchangeRateService)
    }

    // ─── calculateDividendTaxes ────────────────────────────────────────────────

    @Test
    fun `calculateTax - dividend tax9 is 9 percent of UAH brutto`() {
        val dividend = dividendRecord(amount = BigDecimal("100.00"), date = LocalDate.of(2024, 1, 1))
        val rate = BigDecimal("40.00")  // brutto = 4000.00

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(emptyMap(), listOf(dividend)))
        whenever(exchangeRateService.getRateForDate(dividend.date)).thenReturn(rate)
        mockEmptyRepos()

        val savedReport = argumentCaptor<TotalDividendReport>()
        whenever(totalDividendReportRepository.save(savedReport.capture()))
            .thenAnswer { savedReport.firstValue }
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        val report = savedReport.lastValue
        // 4000 * 0.09 = 360.00
        assertEquals(BigDecimal("360.00"), report.totalTax9)
    }

    @Test
    fun `calculateTax - military tax is added when isMilitary is false`() {
        val dividend = dividendRecord(amount = BigDecimal("100.00"), date = LocalDate.of(2024, 1, 1))

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(emptyMap(), listOf(dividend)))
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(BigDecimal("40.00"))
        mockEmptyRepos()

        val savedReport = argumentCaptor<TotalDividendReport>()
        whenever(totalDividendReportRepository.save(savedReport.capture()))
            .thenAnswer { savedReport.firstValue }
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        val report = savedReport.firstValue
        // totalTaxSum = tax9 (360) + militaryTax5 (200) = 560
        assertEquals(BigDecimal("360.00"), report.totalTax9)
        assertEquals(BigDecimal("200.00"), report.totalMilitaryTax5)
        assertEquals(BigDecimal("560.00"), report.totalTaxSum)
    }

    @Test
    fun `calculateTax - military tax is excluded from taxSum when isMilitary is true`() {
        // Note: isMilitary flag only affects per-row taxSum in DividendRecord,
        // NOT the totalTaxSum which always sums tax9 + military5 on the total.
        // This test verifies each row's taxSum excludes military when isMilitary=true.
        val dividend = dividendRecord(amount = BigDecimal("100.00"), date = LocalDate.of(2024, 1, 1))

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(emptyMap(), listOf(dividend)))
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(BigDecimal("40.00"))
        mockEmptyRepos()

        val savedReport = argumentCaptor<TotalDividendReport>()
        whenever(totalDividendReportRepository.save(savedReport.capture()))
            .thenAnswer { savedReport.firstValue }
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = true)

        val report = savedReport.lastValue
        val rowTaxSum = report.records.first().taxSum
        // row taxSum should equal only tax9 (360), no military
        assertEquals(BigDecimal("360.00"), rowTaxSum)
    }

    @Test
    fun `calculateTax - dividend total is calculated on summed UAH brutto, not sum of individual taxes`() {
        // Two dividends each 100 USD @ 40 = 4000 UAH brutto each, total brutto 8000
        // Total tax9 should be 8000 * 0.09 = 720 (not 360+360=720 — same here, but rounding can differ)
        val date1 = LocalDate.of(2024, 1, 1)
        val date2 = LocalDate.of(2024, 2, 1)
        val dividends = listOf(
            dividendRecord(amount = BigDecimal("100.00"), date = date1),
            dividendRecord(amount = BigDecimal("100.00"), date = date2)
        )

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(emptyMap(), dividends))
        whenever(exchangeRateService.getRateForDate(date1)).thenReturn(BigDecimal("40.00"))
        whenever(exchangeRateService.getRateForDate(date2)).thenReturn(BigDecimal("40.00"))
        mockEmptyRepos()

        val savedReport = argumentCaptor<TotalDividendReport>()
        whenever(totalDividendReportRepository.save(savedReport.capture()))
            .thenAnswer { savedReport.firstValue }
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        val report = savedReport.firstValue
        assertEquals(BigDecimal("8000.00"), report.totalUaBrutto)
        assertEquals(BigDecimal("720.00"), report.totalTax9)
    }

    @Test
    fun `calculateTax - reuses existing dividend report if one exists for the year`() {
        val existingReport = TotalDividendReport(year = year, status = ReportStatus.CALCULATED)

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(emptyMap(), emptyList()))
        mockEmptyRepos()
        whenever(totalDividendReportRepository.findByYear(year)).thenReturn(Optional.of(existingReport))
        whenever(totalDividendReportRepository.save(any<TotalDividendReport>()))
            .thenAnswer { it.arguments[0] }
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        // save should be called with the same existing instance
        verify(totalDividendReportRepository).save(existingReport)
    }

    // ─── calculateStockTaxes ───────────────────────────────────────────────────

    @Ignore
    @Test
    fun `calculateTax - stock net profit is sell price minus buy cost in UAH`() {
        val buyDate = LocalDate.of(2024, 1, 1)
        val sellDate = LocalDate.of(2024, 2, 1)

        // 10 shares bought @ $150 cost basis total = -1500, rate 40 => buyPriceUAH = 60000 / 10 each = 6000 total
        // Sell 10 @ $160, rate 42 => sellPriceUAH = 160 * 42 = 6720, minus commission abs(1)*42 = 42 => 6678
        // netProfit = 6678 - 6000 = 678
        val trades = mapOf(
            "AAPL" to listOf(
                stockRecord(
                    buySell = "BUY", tradeDate = buyDate, quantity = 10,
                    costBasis = BigDecimal("-1500.00"), tradePrice = BigDecimal("150.00")
                ),
                stockRecord(
                    buySell = "SELL", tradeDate = sellDate, quantity = 10,
                    tradePrice = BigDecimal("160.00"), ibCommission = BigDecimal("-1.00"),
                    costBasis = BigDecimal("1600.00"), fifoPnlRealized = BigDecimal("100.00")
                )
            )
        )

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(trades, emptyList()))
        whenever(exchangeRateService.getRateForDate(buyDate)).thenReturn(BigDecimal("40.00"))
        whenever(exchangeRateService.getRateForDate(sellDate)).thenReturn(BigDecimal("42.00"))
        mockEmptyRepos()

        val savedReport = argumentCaptor<TotalStockReport>()
        whenever(totalStockReportRepository.save(savedReport.capture()))
            .thenAnswer { savedReport.firstValue }
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        val report = savedReport.lastValue
        // buyPriceUAH: abs(-1500) / 10 * 40 * 10 = 6000
        // sellPriceUAH: 160 * 42 - 1*42 = 6720 - 42 = 6678
        // netProfit = 678
        assertEquals(BigDecimal("6000.00"), report.totalBuy)
        assertEquals(BigDecimal("6678.00"), report.totalSell)
        assertEquals(BigDecimal("678.00"), report.totalUaBrutto)
    }

    @Ignore
    @Test
    fun `calculateTax - stock tax18 is 18 percent of net profit`() {
        val buyDate = LocalDate.of(2024, 1, 1)
        val sellDate = LocalDate.of(2024, 2, 1)

        val trades = mapOf(
            "AAPL" to listOf(
                stockRecord(
                    buySell = "BUY", tradeDate = buyDate, quantity = 10,
                    costBasis = BigDecimal("-1000.00"), tradePrice = BigDecimal("100.00")
                ),
                stockRecord(
                    buySell = "SELL", tradeDate = sellDate, quantity = 10,
                    tradePrice = BigDecimal("200.00"), ibCommission = BigDecimal.ZERO,
                    costBasis = BigDecimal("2000.00")
                )
            )
        )

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(trades, emptyList()))
        whenever(exchangeRateService.getRateForDate(buyDate)).thenReturn(BigDecimal("40.00"))
        whenever(exchangeRateService.getRateForDate(sellDate)).thenReturn(BigDecimal("40.00"))
        mockEmptyRepos()

        val savedReport = argumentCaptor<TotalStockReport>()
        whenever(totalStockReportRepository.save(savedReport.capture()))
            .thenAnswer { savedReport.firstValue }
        whenever(taxReportMapper.toTotalReportDto(any(), any()))
            .thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        val report = savedReport.lastValue
        // buy: 1000/10*40*10 = 4000, sell: 200*40=8000, profit=4000
        // tax18 = 4000 * 0.18 = 720, military5 = 4000 * 0.05 = 200
        assertEquals(BigDecimal("720.00"), report.totalTax18)
        assertEquals(BigDecimal("200.00"), report.totalMilitaryTax5)
        assertEquals(BigDecimal("920.00"), report.totalTaxSum)
    }

    @Ignore
    @Test
    fun `calculateTax - totalUaNetto is brutto minus total tax sum`() {
        val buyDate = LocalDate.of(2024, 1, 1)
        val sellDate = LocalDate.of(2024, 2, 1)

        val trades = mapOf(
            "AAPL" to listOf(
                stockRecord(
                    buySell = "BUY", tradeDate = buyDate, quantity = 10,
                    costBasis = BigDecimal("-1000.00"), tradePrice = BigDecimal("100.00")
                ),
                stockRecord(
                    buySell = "SELL", tradeDate = sellDate, quantity = 10,
                    tradePrice = BigDecimal("200.00"), ibCommission = BigDecimal.ZERO,
                    costBasis = BigDecimal("2000.00")
                )
            )
        )

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(trades, emptyList()))
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(BigDecimal("40.00"))
        mockEmptyRepos()

        val savedReport = argumentCaptor<TotalStockReport>()
        whenever(totalStockReportRepository.save(savedReport.capture()))
            .thenAnswer { savedReport.firstValue }
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        val report = savedReport.lastValue
        // brutto=4000, taxSum=920, netto=3080
        assertEquals(BigDecimal("3080.00"), report.totalUaNetto)
    }

    @Ignore
    @Test
    fun `calculateTax - FIFO queue matches sells to buys in chronological order`() {
        val buyDate1 = LocalDate.of(2024, 1, 1)
        val buyDate2 = LocalDate.of(2024, 2, 1)
        val sellDate = LocalDate.of(2024, 3, 1)

        // Two buy lots at different prices, one sell that consumes first lot (FIFO)
        val trades = mapOf(
            "AAPL" to listOf(
                stockRecord(
                    buySell = "BUY", tradeDate = buyDate1, quantity = 5,
                    costBasis = BigDecimal("-500.00"), tradePrice = BigDecimal("100.00")
                ),
                stockRecord(
                    buySell = "BUY", tradeDate = buyDate2, quantity = 5,
                    costBasis = BigDecimal("-750.00"), tradePrice = BigDecimal("150.00")
                ),
                stockRecord(
                    buySell = "SELL", tradeDate = sellDate, quantity = 5,
                    tradePrice = BigDecimal("200.00"), ibCommission = BigDecimal.ZERO,
                    costBasis = BigDecimal("1000.00")
                )
            )
        )

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(trades, emptyList()))
        whenever(exchangeRateService.getRateForDate(buyDate1)).thenReturn(BigDecimal("40.00"))
        whenever(exchangeRateService.getRateForDate(buyDate2)).thenReturn(BigDecimal("40.00"))
        whenever(exchangeRateService.getRateForDate(sellDate)).thenReturn(BigDecimal("40.00"))
        mockEmptyRepos()

        val savedReport = argumentCaptor<TotalStockReport>()
        whenever(totalStockReportRepository.save(savedReport.capture()))
            .thenAnswer { savedReport.firstValue }
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        val report = savedReport.lastValue
        // FIFO: sell 5 uses first buy lot (500/5*40*5 = 2000 UAH buy cost)
        // sell: 200*40 = 8000, profit = 6000
        assertEquals(BigDecimal("2000.00"), report.totalBuy)
        assertEquals(BigDecimal("8000.00"), report.totalSell)
    }

    @Test
    fun `calculateTax - reuses existing stock report if one exists for the year`() {
        val existingReport = TotalStockReport(year = year, status = ReportStatus.CALCULATED)

        whenever(parser.parseIbCsv(any())).thenReturn(Pair(emptyMap(), emptyList()))
        mockEmptyRepos()
        whenever(totalStockReportRepository.findByYear(year)).thenReturn(Optional.of(existingReport))
        whenever(totalStockReportRepository.save(any<TotalStockReport>())).thenAnswer { it.arguments[0] }
        whenever(taxReportMapper.toTotalReportDto(any(), any())).thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        verify(totalStockReportRepository).save(existingReport)
    }

    // ─── deleteStockReport ─────────────────────────────────────────────────────

    @Test
    fun `deleteStockReport - deletes report when it exists for year`() {
        val report = TotalStockReport(id = 1L, year = year, status = ReportStatus.CALCULATED)
        whenever(totalStockReportRepository.findByYear(year)).thenReturn(Optional.of(report))

        taxService.deleteStockReport(year)

        verify(totalStockReportRepository).delete(report)
    }

    @Test
    fun `deleteStockReport - does nothing when no report exists for year`() {
        whenever(totalStockReportRepository.findByYear(year)).thenReturn(Optional.empty())

        taxService.deleteStockReport(year)

        verify(totalStockReportRepository, never()).delete(any<TotalStockReport>())
    }

    // ─── deleteDividendReport ──────────────────────────────────────────────────

    @Test
    fun `deleteDividendReport - deletes report when it exists for year`() {
        val report = TotalDividendReport(id = 1L, year = year, status = ReportStatus.CALCULATED)
        whenever(totalDividendReportRepository.findByYear(year)).thenReturn(Optional.of(report))

        taxService.deleteDividendReport(year)

        verify(totalDividendReportRepository).delete(report)
    }

    @Test
    fun `deleteDividendReport - does nothing when no report exists for year`() {
        whenever(totalDividendReportRepository.findByYear(year)).thenReturn(Optional.empty())

        taxService.deleteDividendReport(year)

        verify(totalDividendReportRepository, never()).delete(any<TotalDividendReport>())
    }

    @Test
    fun `calculateTax - empty trades and dividends produces zero totals`() {
        whenever(parser.parseIbCsv(any())).thenReturn(Pair(emptyMap(), emptyList()))
        mockEmptyRepos()

        val savedStockReport = argumentCaptor<TotalStockReport>()
        val savedDividendReport = argumentCaptor<TotalDividendReport>()
        whenever(totalStockReportRepository.save(savedStockReport.capture()))
            .thenAnswer { savedStockReport.firstValue }
        whenever(totalDividendReportRepository.save(savedDividendReport.capture()))
            .thenAnswer { savedDividendReport.firstValue }
        whenever(taxReportMapper.toTotalReportDto(any(), any()))
            .thenReturn(TotalTaxReportDto())

        taxService.calculateTax(year, csvFile(), FileType.CSV, isMilitary = false)

        assertEquals(BigDecimal.ZERO, savedStockReport.firstValue.totalBuy)
        assertEquals(BigDecimal.ZERO, savedDividendReport.firstValue.totalUaBrutto)
    }
}