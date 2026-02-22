package com.kotlin.assets.service

import com.kotlin.assets.entity.SolarFileReport
import com.kotlin.assets.repository.SolarFileReportRepository
import com.kotlin.assets.repository.SolarRepository
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.ui.Model
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
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

    @BeforeEach
    fun setup() {
        whenever(solarFileReportRepository.save(any<SolarFileReport>())).thenReturn(fileReport)
    }

    @Test
    fun `should save file report with correct userId and fileName`() {
        val inputStream = buildXlsxInputStream(
        )
        whenever(file.inputStream).thenReturn(inputStream)
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(BigDecimal("38.00"))
        whenever(solarRepository.upsert(any(), any(), any(), any(), any(), any())).thenReturn(1L)

        solarReportService.calculateGreenReturn(file, model, fileName, userId)

        verify(solarFileReportRepository).save(
            argThat { it.fileName == fileName && it.userId == userId }
        )
    }

    @Test
    fun `should add reports to model`() {
        val inputStream = buildXlsxInputStream(
        )
        whenever(file.inputStream).thenReturn(inputStream)
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(BigDecimal("38.00"))
        whenever(solarRepository.upsert(any(), any(), any(), any(), any(), any())).thenReturn(1L)

        solarReportService.calculateGreenReturn(file, model, fileName, userId)

        verify(model).addAttribute(eq("reports"), any())
        verify(model).addAttribute(eq("total"), any())
        verify(model).addAttribute(eq("usTotal"), any())
    }

    @Test
    fun `should calculate correct totals for single row`() {
        val inputStream = buildXlsxInputStream()
        val exchangeRate = BigDecimal("40.00")
        val expectedUsdValue = BigDecimal("25.00") // 1000 / 40

        whenever(file.inputStream).thenReturn(inputStream)
        whenever(exchangeRateService.getRateForDate(any())).thenReturn(exchangeRate)
        whenever(solarRepository.upsert(any(), any(), any(), any(), any(), any())).thenReturn(1L)

        solarReportService.calculateGreenReturn(file, model, fileName, userId)

        verify(model).addAttribute("total", "Total Amount: 1000.00 ₴")
        verify(model).addAttribute("usTotal", "Total US Amount: $expectedUsdValue $")
    }

    @Test
    fun `should skip rows with invalid date format`() {
        val inputStream = buildXlsxInputStream(
        )
        whenever(file.inputStream).thenReturn(inputStream)
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

        val inputStream = buildXlsxInputStream(
        )
        whenever(file.inputStream).thenReturn(inputStream)
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
        whenever(file.inputStream).thenThrow(RuntimeException("corrupted"))

        assertThrows<IllegalArgumentException> {
            solarReportService.calculateGreenReturn(file, model, fileName, userId)
        }
    }

    @Test
    fun `should not call upsert when file has no valid rows`() {
        val inputStream = buildXlsxInputStream()
        whenever(file.inputStream).thenReturn(inputStream)

        solarReportService.calculateGreenReturn(file, model, fileName, userId)

        verify(solarRepository, never()).upsert(any(), any(), any(), any(), any(), any())
    }

    // put a real test.xlsx in src/test/resources/
    fun buildXlsxInputStream(): InputStream {
        return javaClass.classLoader.getResourceAsStream("Test(1).xlsx")
            ?: error("test.xlsx not found in resources")
    }
}