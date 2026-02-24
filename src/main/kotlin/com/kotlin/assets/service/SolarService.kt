package com.kotlin.assets.service

import com.kotlin.assets.dto.green.MonthSummary
import com.kotlin.assets.dto.green.Statistics
import com.kotlin.assets.dto.green.YearSummary
import com.kotlin.assets.entity.SolarFileReport
import com.kotlin.assets.entity.SolarReport
import com.kotlin.assets.repository.SolarFileReportRepository
import com.kotlin.assets.repository.SolarRepository
import mu.KotlinLogging
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class SolarService(
    val exchangeRateService: ExchangeRateService,
    val solarRepository: SolarRepository,
    val solarFileReportRepository: SolarFileReportRepository
) {

    private val logger = KotlinLogging.logger {}

    @Transactional
    fun calculateGreenReturn(
        file: MultipartFile,
        model: Model,
        fileName: String,
        userId: Long,
        skipRow: Boolean = false
    ) {
        try {

            // Always create a new file report entry as an audit record
            val fileReport = solarFileReportRepository.save(
                SolarFileReport(fileName = fileName, userId = userId)
            )

            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
            var total = BigDecimal.ZERO
            var usdTotal = BigDecimal.ZERO
            //Read file
            val reports = DataFrame.readExcel(file.inputStream, skipRows = if (skipRow) 1 else 0)
                .filter { row ->
                    val dateStr = row[0].toString().trim()
                    dateStr.matches(Regex("""\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}"""))
                }
                .map { row ->
                    val date = LocalDateTime.parse(row[0].toString().trim(), formatter).toLocalDate()
                    val exchangeRate = exchangeRateService.getRateForDate(date)
                    val amount = row[4].toString().trim()
                        .replace(" ", "")
                        .replace(",", ".")
                        .toBigDecimal()
                    total += amount
                    val usdValue = amount.divide(exchangeRate, 2, RoundingMode.HALF_UP)
                    usdTotal += usdValue

                    val reportId =
                        solarRepository.upsert(
                            date = date,
                            amount = amount,
                            solarFileReportId = fileReport.id!!,
                            year = date.year,
                            exchangeRate = exchangeRate,
                            usdValue = usdValue
                        )

                    SolarReport(
                        id = reportId,
                        year = date.year,
                        date = date,
                        amount = amount,
                        exchangeRate = exchangeRate,
                        usdValue = usdValue
                    )
                }

            fileReport.total = total
            fileReport.usdTotal = usdTotal

            model.addAttribute("reports", reports)
            model.addAttribute("total", "Total Amount: $total ₴")
            model.addAttribute("usTotal", "Total US Amount: $usdTotal $")
        } catch (e: Exception) {
            logger.info("Error {e}", e)
            throw IllegalArgumentException("Invalid or corrupted xlsx file")
        }
    }

    fun getAllReports(model: Model, userId: Long) {
        val fileReport = solarFileReportRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
            .orElseThrow { throw ResponseStatusException(HttpStatus.NOT_FOUND) }
        val total = fileReport.total
        val usdTotal = fileReport.usdTotal

        val reports = solarRepository.findAllBySolarFileReportId(fileReport.id)

        model.addAttribute("reports", reports)
        model.addAttribute("total", "Total Amount: $total ₴")
        model.addAttribute("usTotal", "Total US Amount: $usdTotal $")
    }

    fun buildStatistics(): Statistics {
        val records = solarRepository.findAll()
        val byYear = records
            .groupBy { it.date.year }
            .map { (year, yearRecords) ->
                val byMonth = yearRecords
                    .groupBy { it.date.month }
                    .map { (month, monthRecords) ->
                        MonthSummary(
                            month = month,
                            total = monthRecords.sumOf { it.amount },
                            usdTotal = monthRecords.sumOf { it.usdValue },
                            count = monthRecords.size
                        )
                    }
                    .sortedBy { it.month }

                YearSummary(
                    year = year,
                    total = yearRecords.sumOf { it.amount },
                    usdTotal = yearRecords.sumOf { it.usdValue },
                    count = yearRecords.size,
                    byMonth = byMonth
                )
            }
            .sortedByDescending { it.year }

        return Statistics(
            grandTotal = records.sumOf { it.amount },
            grandUsdTotal = records.sumOf { it.usdValue },
            byYear = byYear
        )
    }
}