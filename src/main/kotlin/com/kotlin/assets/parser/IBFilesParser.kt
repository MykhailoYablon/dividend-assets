package com.kotlin.assets.parser

import com.kotlin.assets.dto.enums.SectionType
import com.kotlin.assets.dto.ib.DividendRecord
import com.kotlin.assets.dto.ib.TradeRecord
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

@Service
class IBFilesParser {

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss")

    fun parseIbCsv(file: MultipartFile): Pair<Map<String, List<TradeRecord>>, MutableList<DividendRecord>> {
        val tradeRecords = mutableListOf<TradeRecord>()
        val dividendRecords = mutableListOf<DividendRecord>()

        var currentHeaders: Map<String, Int> = emptyMap()
        var currentType: SectionType = SectionType.UNKNOWN

        file.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                if (line.isBlank()) return@forEach

                val row = parseCsvLine(line)

                if (row.firstOrNull() == "ClientAccountID") {
                    currentHeaders = row.mapIndexed { index, name -> name to index }.toMap()
                    currentType = detectSectionType(currentHeaders)
                    return@forEach
                }

                if (currentHeaders.isEmpty()) return@forEach

                fun col(name: String) = currentHeaders[name]?.let { row.getOrElse(it) { "" } } ?: ""

                when (currentType) {
                    SectionType.TRADES -> runCatching {
                        if (col("AssetClass") == "STK")
                            tradeRecords.add(
                                TradeRecord(
                                    symbol = col("Symbol"),
                                    tradeDate = LocalDate.parse(
                                        col("TradeDate"),
                                        DateTimeFormatter.ofPattern("yyyyMMdd")
                                    ),
                                    quantity = col("Quantity").toBigDecimal(),
                                    tradePrice = col("TradePrice").toBigDecimal(),
                                    costBasis = col("CostBasis").toBigDecimal(),
                                    ibCommission = col("IBCommission").toBigDecimal(),
                                    fifoPnlRealized = col("FifoPnlRealized").toBigDecimal(),
                                    buySell = col("Buy/Sell")
                                )
                            )
                    }

                    SectionType.DIVIDENDS -> runCatching {
                        if (col("Type") == "Dividends")
                            dividendRecords.add(
                                DividendRecord(
                                    symbol = col("Symbol"),
                                    date = LocalDate.parse(
                                        col("Date/Time").substringBefore(";"),
                                        DateTimeFormatter.ofPattern("yyyyMMdd")
                                    ),
                                    amount = col("Amount").toBigDecimal(),
                                    type = col("Type"),
                                    description = col("Description")
                                )
                            )
                    }

                    SectionType.UNKNOWN -> return@forEach
                }
            }
        }

        val closedTrades = tradeRecords
            .sortedBy { it.tradeDate }
            .groupBy { it.symbol }
            .filter { (_, trades) -> trades.any { it.buySell == "SELL" } }

        return Pair(closedTrades, dividendRecords)
    }

    private fun detectSectionType(headers: Map<String, Int>): SectionType = when {
        headers.containsKey("TradeDate") -> SectionType.TRADES
        headers.containsKey("Date/Time") -> SectionType.DIVIDENDS
        else -> SectionType.UNKNOWN
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }

                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }

    fun parseIbXml(file: MultipartFile): Pair<Map<String, List<TradeRecord>>, List<DividendRecord>> {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file.inputStream)

        doc.documentElement.normalize()

        fun Node.attr(name: String) = (this as Element).getAttribute(name)

        val tradeRecords = doc.getElementsByTagName("Trade")
            .toList()
            .filter { node -> node.attr("assetCategory") == "STK" }
            .mapNotNull { node ->
                runCatching {
                    TradeRecord(
                        symbol = node.attr("symbol"),
                        tradeDate = LocalDate.parse(
                            node.attr("tradeDate"),
                            DateTimeFormatter.ofPattern("yyyyMMdd")
                        ),
                        quantity = node.attr("quantity").toBigDecimal(),
                        tradePrice = node.attr("tradePrice").toBigDecimal(),
                        costBasis = node.attr("cost").toBigDecimal(),
                        ibCommission = node.attr("ibCommission").toBigDecimal(),
                        fifoPnlRealized = node.attr("fifoPnlRealized").toBigDecimal(),
                        buySell = node.attr("buySell")
                    )
                }.getOrNull()
            }

        val dividendRecords = doc.getElementsByTagName("CashTransaction")
            .toList()
            .filter { node -> node.attr("type") == "Dividends" }
            .mapNotNull { node ->
                runCatching {
                    DividendRecord(
                        symbol = node.attr("symbol"),
                        date = LocalDate.parse(
                            node.attr("dateTime").substringBefore(";"),
                            DateTimeFormatter.ofPattern("yyyyMMdd")
                        ),
                        amount = node.attr("amount").toBigDecimal(),
                        type = node.attr("type"),
                        description = node.attr("description")
                    )
                }.getOrNull()
            }

        val closedTrades = tradeRecords
            .sortedBy { it.tradeDate }
            .groupBy { it.symbol }
            .filter { (_, trades) -> trades.any { it.buySell == "SELL" } }

        return Pair(closedTrades, dividendRecords)
    }

    // Extension to convert NodeList to a regular List
    fun NodeList.toList(): List<Node> = (0 until length).map { item(it) }
}