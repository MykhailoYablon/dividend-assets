package com.kotlin.assets.parser

import com.kotlin.assets.dto.ib.IBRecord
import com.kotlin.assets.dto.ib.IbData
import com.kotlin.assets.dto.ib.ParsedData
import com.opencsv.bean.CsvToBeanBuilder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.StringReader
import java.time.LocalDate
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.map

@Service
class IBFilesParser {

    val SYMBOL_PATTERN: Pattern = Pattern.compile("^([A-Z]+)(?=\\()")

    fun parseDividendFile(ibReportFile: MultipartFile): ParsedData {
        var currentSection: String? = null
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
                        }
                    }
                }
            }

        }
        val dividends = parseIbRecords(dividendLines)
        val withholdingTax = parseIbRecords(withholdingTaxLines)

        return ParsedData(dividends, withholdingTax)
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

    private fun extractSymbol(description: String): String {
        val matcher: Matcher = SYMBOL_PATTERN.matcher(description)
        return if (matcher.find()) matcher.group(1) else ""
    }

}