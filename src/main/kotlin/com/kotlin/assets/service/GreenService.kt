package com.kotlin.assets.service

import com.kotlin.assets.dto.green.GreenReturnReport
import mu.KotlinLogging
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class GreenService(val exchangeRateService: ExchangeRateService) {

    private val logger = KotlinLogging.logger {}

    fun calculateGreenReturn(file: MultipartFile, model: Model) {
        try {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
            var total = BigDecimal.ZERO
            var usTotal = BigDecimal.ZERO
            val reports = DataFrame.readExcel(file.inputStream, skipRows = 1)
                .filter { row ->
                    val dateStr = row["Дата"].toString().trim()
                    dateStr.matches(Regex("""\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}"""))
                }
                .map { row ->
                    val date = LocalDateTime.parse(row[0].toString().trim(), formatter)

                    val exchangeRate = exchangeRateService.getRateForDate(date.toLocalDate())

                    val amount = row[4].toString().trim()
                        .replace(" ", "")
                        .replace(",", ".")
                        .toBigDecimal()
                    total += amount
                    val usdValue = amount.divide(exchangeRate, 2, RoundingMode.HALF_UP)
                    usTotal += usdValue
                    GreenReturnReport(
                        year = date.year,
                        date = date,
                        amount = amount,
                        exchangeRate = exchangeRate,
                        usdValue = usdValue,
                    )

                }

            model.addAttribute("reports", reports)
            model.addAttribute("total", "Total Amount: $total")
            model.addAttribute("usTotal", "Total US Amount: $usTotal")
        } catch (e: Exception) {
            logger.info("Error {e}", e)
        }
    }
}