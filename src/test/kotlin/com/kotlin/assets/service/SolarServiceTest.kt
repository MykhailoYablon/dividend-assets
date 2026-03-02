package com.kotlin.assets.service

import com.kotlin.assets.entity.solar.SolarFileReport
import com.kotlin.assets.entity.solar.SolarReport
import com.kotlin.assets.repository.SolarFileReportRepository
import com.kotlin.assets.repository.SolarRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.ui.Model
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.util.Optional
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class SolarServiceTest {

    @Mock
    private lateinit var solarFileReportRepository: SolarFileReportRepository

    @Mock
    private lateinit var solarRepository: SolarRepository

    @Mock
    private lateinit var exchangeRateService: ExchangeRateService

    @InjectMocks
    private lateinit var solarReportService: SolarService

    @Mock
    private lateinit var file: MultipartFile

    @Mock
    private lateinit var model: Model

    private val userId = 1L
    private val fileName = "test.xlsx"
    private val fileReport = SolarFileReport(id = 1L, fileName = fileName, userId = userId)

    // Test data helpers
    private fun createReport(
        date: LocalDate,
        amount: BigDecimal,
        usdValue: BigDecimal
    ) = SolarReport(
        date = date,
        amount = amount,
        usdValue = usdValue,
        year = date.year
    )

    @Test
    fun `should save file report with correct userId and fileName`() {
        val file = buildXlsxMultipartFile(listOf(listOf("01.01.2024 00:00:00", "", "", "", "1000,00")))
        whenever(solarFileReportRepository.save(any<SolarFileReport>())).thenReturn(fileReport)
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(BigDecimal("38.00"))
        whenever(solarRepository.upsert(any(), any(), any(), any(), any(), any())).thenReturn(1L)

        solarReportService.calculateGreenReturn(file, model, fileName, userId)

        verify(solarFileReportRepository).save(
            argThat { it.fileName == fileName && it.userId == userId }
        )
    }

    @Test
    fun `should add reports to model`() {
        val file = buildXlsxMultipartFile(
            listOf(listOf("01.01.2024 00:00:00", "", "", "", "1000,00"))
        )
        whenever(solarFileReportRepository.save(any<SolarFileReport>())).thenReturn(fileReport)
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(BigDecimal("38.00"))
        whenever(solarRepository.upsert(any(), any(), any(), any(), any(), any())).thenReturn(1L)

        solarReportService.calculateGreenReturn(file, model, fileName, userId)

        verify(model).addAttribute(eq("reports"), any())
        verify(model).addAttribute(eq("total"), any())
        verify(model).addAttribute(eq("usTotal"), any())
    }

    @Test
    fun `should calculate correct totals for single row`() {
        val file = buildXlsxMultipartFile(listOf(listOf("01.01.2024 00:00:00", "", "", "", "1 000,00")))
        val exchangeRate = BigDecimal("40.00")
        val expectedUsdValue = BigDecimal("25.00") // 1000 / 40

        whenever(solarFileReportRepository.save(any<SolarFileReport>())).thenReturn(fileReport)
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(exchangeRate)
        whenever(solarRepository.upsert(any(), any(), any(), any(), any(), any())).thenReturn(1L)

        solarReportService.calculateGreenReturn(file, model, fileName, userId)

        verify(model).addAttribute("total", "Total Amount: 1000.00 ₴")
        verify(model).addAttribute("usTotal", "Total US Amount: $expectedUsdValue $")
    }

    @Test
    fun `should skip rows with invalid date format`() {
        val file = buildXlsxMultipartFile(
            listOf(
                listOf("invalid-date", "", "", "", "1000,00"),
                listOf("01.01.2024 00:00:00", "", "", "", "500,00")
            )
        )
        whenever(solarFileReportRepository.save(any<SolarFileReport>())).thenReturn(fileReport)
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(BigDecimal("38.00"))
        whenever(solarRepository.upsert(any(), any(), any(), any(), any(), any())).thenReturn(1L)

        solarReportService.calculateGreenReturn(file, model, fileName, userId)

        // upsert should only be called once — for the valid row
        verify(solarRepository, times(1)).upsert(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `should call upsert with correct parameters`() {
        val expectedDate = LocalDate.of(2024, 1, 1)
        val expectedAmount = BigDecimal("1000.00")
        val expectedRate = BigDecimal("38.00")
        val expectedUsd = expectedAmount.divide(expectedRate, 2, RoundingMode.HALF_UP)

        val file = buildXlsxMultipartFile(
            listOf(listOf("01.01.2024 00:00:00", "", "", "", "1000,00"))
        )
        whenever(solarFileReportRepository.save(any<SolarFileReport>())).thenReturn(fileReport)
        whenever(exchangeRateService.getRateForDate(expectedDate)).thenReturn(expectedRate)
        whenever(solarRepository.upsert(any(), any(), any(), any(), any(), any())).thenReturn(1L)

        solarReportService.calculateGreenReturn(file, model, fileName, userId)

        verify(solarRepository).upsert(
            date = expectedDate,
            amount = expectedAmount,
            solarFileReportId = fileReport.id!!,
            year = 2024,
            exchangeRate = expectedRate,
            usdValue = expectedUsd
        )
    }

    @Test
    fun `should throw IllegalArgumentException when file is corrupted`() {

        assertThrows<IllegalArgumentException> {
            solarReportService.calculateGreenReturn(file, model, fileName, userId)
        }
    }

    @Test
    fun `should not call upsert when file has no valid rows`() {
        val file = buildXlsxMultipartFile(emptyList())

        assertThrows<IllegalArgumentException> {
            solarReportService.calculateGreenReturn(file, model, fileName, userId)
        }

        verify(solarRepository, never()).upsert(any(), any(), any(), any(), any(), any())
    }

    fun buildXlsxMultipartFile(rows: List<List<Any>>): MultipartFile {
        val out = ByteArrayOutputStream()
        val dataRows = rows.map { row ->
            mapOf(
                "Дата" to row.getOrElse(0) { "" }.toString(),
                "Категорія" to row.getOrElse(1) { "" }.toString(),
                "Картка" to row.getOrElse(2) { "" }.toString(),
                "Опис операції" to row.getOrElse(3) { "" }.toString(),
                "Сума в валюті картки" to row.getOrElse(4) { "" }.toString(),
                "Валюта картки" to row.getOrElse(5) { "" }.toString(),
            )
        }
        val header = mutableListOf("Історія операцій за період 01.04.2022 - 20.02.2026")

        val headerFrame = header.toDataFrame()

        val df = dataRows.toDataFrame()

//        val concat = headerFrame.concat(df)

        // Write the DataFrame to the OutputStream
        val wb = WorkbookFactory.create(true)

        df.writeExcel(outputStream = out, factory = wb)

        // Create the MockMultipartFile
        return MockMultipartFile(
            "file", // Name of the request parameter on the server side (e.g., @RequestParam("file"))
            "test.xlsx", // Original filename
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // Content type for .xlsx
            ByteArrayInputStream(out.toByteArray()) // File content as InputStream
        )
    }

    // ---- getAllReports ----

    @Test
    fun `getAllReports should add reports to model`() {
        val reports = listOf(
            createReport(LocalDate.of(2024, 1, 1), BigDecimal("1000"), BigDecimal("25")),
            createReport(LocalDate.of(2024, 2, 1), BigDecimal("2000"), BigDecimal("50"))
        )
        whenever (solarFileReportRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(
            Optional.of(fileReport))
        whenever(solarRepository.findAllBySolarFileReportId(1L)).thenReturn(reports)

        solarReportService.getAllReports(model, userId)

        verify(model).addAttribute("reports", reports)
    }

    @Test
    fun `getAllReports should add empty list to model when no records`() {
        whenever(solarFileReportRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(
            Optional.of(fileReport))
        whenever(solarRepository.findAllBySolarFileReportId(1L)).thenReturn(emptyList())

        solarReportService.getAllReports(model, userId)

        verify(model).addAttribute("reports", emptyList<SolarReport>())
    }

    // ---- buildStatistics ----

    @Test
    fun `buildStatistics should return empty statistics when no records`() {
        whenever(solarRepository.findAll()).thenReturn(emptyList())

        val result = solarReportService.buildStatistics()

        assertThat(result.grandTotal).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.grandUsdTotal).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.byYear).isEmpty()
    }

    @Test
    fun `buildStatistics should calculate correct grand totals`() {
        val records = listOf(
            createReport(LocalDate.of(2024, 1, 1), BigDecimal("1000"), BigDecimal("25")),
            createReport(LocalDate.of(2024, 2, 1), BigDecimal("2000"), BigDecimal("50")),
            createReport(LocalDate.of(2023, 3, 1), BigDecimal("500"), BigDecimal("12"))
        )
        whenever(solarRepository.findAll()).thenReturn(records)

        val result = solarReportService.buildStatistics()

        assertThat(result.grandTotal).isEqualByComparingTo(BigDecimal("3500"))
        assertThat(result.grandUsdTotal).isEqualByComparingTo(BigDecimal("87"))
    }

    @Test
    fun `buildStatistics should group records by year sorted descending`() {
        val records = listOf(
            createReport(LocalDate.of(2022, 1, 1), BigDecimal("100"), BigDecimal("3")),
            createReport(LocalDate.of(2024, 1, 1), BigDecimal("1000"), BigDecimal("25")),
            createReport(LocalDate.of(2023, 1, 1), BigDecimal("500"), BigDecimal("12")),
        )
        whenever(solarRepository.findAll()).thenReturn(records)

        val result = solarReportService.buildStatistics()

        assertThat(result.byYear.map { it.year }).containsExactly(2024, 2023, 2022)
    }

    @Test
    fun `buildStatistics should calculate correct year totals`() {
        val records = listOf(
            createReport(LocalDate.of(2024, 1, 1), BigDecimal("1000"), BigDecimal("25")),
            createReport(LocalDate.of(2024, 2, 1), BigDecimal("2000"), BigDecimal("50")),
        )
        whenever(solarRepository.findAll()).thenReturn(records)

        val result = solarReportService.buildStatistics()

        val year2024 = result.byYear.first { it.year == 2024 }
        assertThat(year2024.total).isEqualByComparingTo(BigDecimal("3000"))
        assertThat(year2024.usdTotal).isEqualByComparingTo(BigDecimal("75"))
        assertThat(year2024.count).isEqualTo(2)
    }

    @Test
    fun `buildStatistics should group records by month sorted ascending within year`() {
        val records = listOf(
            createReport(LocalDate.of(2024, 3, 1), BigDecimal("300"), BigDecimal("7")),
            createReport(LocalDate.of(2024, 1, 1), BigDecimal("100"), BigDecimal("3")),
            createReport(LocalDate.of(2024, 2, 1), BigDecimal("200"), BigDecimal("5")),
        )
        whenever(solarRepository.findAll()).thenReturn(records)

        val result = solarReportService.buildStatistics()

        val months = result.byYear.first().byMonth.map { it.month }
        assertThat(months).containsExactly(Month.JANUARY, Month.FEBRUARY, Month.MARCH)
    }

    @Test
    fun `buildStatistics should calculate correct month totals`() {
        val records = listOf(
            createReport(LocalDate.of(2024, 1, 1), BigDecimal("1000"), BigDecimal("25")),
            createReport(LocalDate.of(2024, 1, 15), BigDecimal("500"), BigDecimal("12")),
            createReport(LocalDate.of(2024, 2, 1), BigDecimal("2000"), BigDecimal("50")),
        )
        whenever(solarRepository.findAll()).thenReturn(records)

        val result = solarReportService.buildStatistics()

        val year2024 = result.byYear.first { it.year == 2024 }
        val january = year2024.byMonth.first { it.month == Month.JANUARY }
        assertThat(january.total).isEqualByComparingTo(BigDecimal("1500"))
        assertThat(january.usdTotal).isEqualByComparingTo(BigDecimal("37"))
        assertThat(january.count).isEqualTo(2)
    }

    @Test
    fun `buildStatistics should handle multiple years with multiple months`() {
        val records = listOf(
            createReport(LocalDate.of(2023, 6, 1), BigDecimal("600"), BigDecimal("15")),
            createReport(LocalDate.of(2023, 7, 1), BigDecimal("700"), BigDecimal("17")),
            createReport(LocalDate.of(2024, 1, 1), BigDecimal("1000"), BigDecimal("25")),
            createReport(LocalDate.of(2024, 1, 20), BigDecimal("400"), BigDecimal("10")),
        )
        whenever(solarRepository.findAll()).thenReturn(records)

        val result = solarReportService.buildStatistics()

        assertThat(result.byYear).hasSize(2)

        val year2024 = result.byYear.first { it.year == 2024 }
        assertThat(year2024.byMonth).hasSize(1)
        assertThat(year2024.byMonth.first().count).isEqualTo(2)

        val year2023 = result.byYear.first { it.year == 2023 }
        assertThat(year2023.byMonth).hasSize(2)
        assertThat(year2023.total).isEqualByComparingTo(BigDecimal("1300"))
    }
}