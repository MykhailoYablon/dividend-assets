package com.kotlin.assets.parser

import com.kotlin.assets.dto.ib.*
import com.opencsv.bean.CsvToBeanBuilder
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.StringReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher
import java.util.regex.Pattern

@Service
class IBFilesParser {

    val SYMBOL_PATTERN: Pattern = Pattern.compile("^([A-Z]+)(?=\\()")

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss")


    fun parseIBFile(ibReportFile: MultipartFile): ParsedData {
        var currentSection: String? = null
        val tradeLines = mutableListOf<String>()
        val dividendLines = mutableListOf<String>()
        val withholdingTaxLines = mutableListOf<String>()

        ibReportFile.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val parts = line.split(",", limit = 2)

                if (parts.isNotEmpty()) {
                    val firstColumn = parts[0].trim()

                    when {
                        line.contains(",Header,") -> currentSection = firstColumn
                        line.contains(",Data,") -> when (currentSection) {
                            "Dividends" -> dividendLines.add(line)
                            "Withholding Tax" -> withholdingTaxLines.add(line)
                            "Trades" -> {
                                val parts = line.split(",", limit = 5)
                                if (parts[3].trim() == "Stocks") {
                                    tradeLines.add(line)
                                }
                            }
                        }
                    }
                }
            }

        }
        val dividends = parseIbRecords(dividendLines)
        val withholdingTax = parseIbRecords(withholdingTaxLines)
        val stockTrades = parseTradeRecords(tradeLines)

        return ParsedData(dividends, withholdingTax, stockTrades)
    }

    private fun parseIbRecords(lines: MutableList<String>): MutableList<IbData> {
        if (lines.isEmpty()) return mutableListOf()

        val csv = lines.joinToString("\n")

        return try {
            StringReader(csv).use { stringReader ->
                CsvToBeanBuilder<IBRecord>(stringReader)
                    .withType(IBRecord::class.java)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse()
                    .filter { r -> !r.date.isEmpty() }
                    .map { r ->
                        IbData(extractSymbol(r.description), LocalDate.parse(r.date), r.amount)
                    }
                    .toMutableList()
            }
        } catch (e: Exception) {
            throw RuntimeException("Error parsing dividends", e)
        }
    }

    private fun parseTradeRecords(lines: MutableList<String>): MutableList<TradeData> {
        if (lines.isEmpty()) return mutableListOf()

        val csv = lines.joinToString("\n")

        return try {
            StringReader(csv).use { stringReader ->
                CsvToBeanBuilder<IBTrade>(stringReader)
                    .withType(IBTrade::class.java)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse()
                    .map { r ->
                        TradeData(r.symbol, LocalDate.parse(r.date, formatter), r.basis, r.realizedPL, r.code)
                    }
                    .toMutableList()
            }
        } catch (e: Exception) {
            throw RuntimeException("Error parsing trades", e)
        }
    }

    private fun extractSymbol(description: String): String {
        val matcher: Matcher = SYMBOL_PATTERN.matcher(description)
        return if (matcher.find()) matcher.group(1) else ""
    }

}