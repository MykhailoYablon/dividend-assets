package com.kotlin.assets.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile

class IBFilesParserTest {

    private val parser = IBFilesParser()

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun csvFile(content: String): MultipartFile =
        MockMultipartFile("file", "trades.csv", "text/csv", content.trimIndent().toByteArray())

    private fun xmlFile(content: String): MultipartFile =
        MockMultipartFile("file", "trades.xml", "application/xml", content.trimIndent().toByteArray())

    // ─── CSV – Trades ──────────────────────────────────────────────────────────

    @Test
    fun `parseIbCsv - returns closed stock trades grouped by symbol`() {
        val csv = """
            ClientAccountID,Symbol,AssetClass,TradeDate,Quantity,TradePrice,CostBasis,IBCommission,FifoPnlRealized,Buy/Sell
            U123,AAPL,STK,20240101,10,150.00,-1500.00,-1.00,0.00,BUY
            U123,AAPL,STK,20240201,10,160.00,1600.00,-1.00,100.00,SELL
        """.trimIndent()

        val (trades, dividends) = parser.parseIbCsv(csvFile(csv))

        assertEquals(1, trades.size)
        assertTrue(trades.containsKey("AAPL"))
        assertEquals(2, trades["AAPL"]!!.size)
        assertTrue(dividends.isEmpty())
    }

    @Test
    fun `parseIbCsv - BUY-only symbol is excluded from closed trades`() {
        val csv = """
            ClientAccountID,Symbol,AssetClass,TradeDate,Quantity,TradePrice,CostBasis,IBCommission,FifoPnlRealized,Buy/Sell
            U123,MSFT,STK,20240101,5,300.00,-1500.00,-1.00,0.00,BUY
        """.trimIndent()

        val (trades, _) = parser.parseIbCsv(csvFile(csv))

        assertTrue(trades.isEmpty())
    }

    @Test
    fun `parseIbCsv - non-STK asset classes are ignored`() {
        val csv = """
            ClientAccountID,Symbol,AssetClass,TradeDate,Quantity,TradePrice,CostBasis,IBCommission,FifoPnlRealized,Buy/Sell
            U123,SPY,OPT,20240101,1,5.00,-500.00,-0.50,0.00,SELL
        """.trimIndent()

        val (trades, _) = parser.parseIbCsv(csvFile(csv))

        assertTrue(trades.isEmpty())
    }

    @Test
    fun `parseIbCsv - trades are sorted by date within each symbol group`() {
        val csv = """
            ClientAccountID,Symbol,AssetClass,TradeDate,Quantity,TradePrice,CostBasis,IBCommission,FifoPnlRealized,Buy/Sell
            U123,AAPL,STK,20240301,10,170.00,1700.00,-1.00,200.00,SELL
            U123,AAPL,STK,20240101,10,150.00,-1500.00,-1.00,0.00,BUY
        """.trimIndent()

        val (trades, _) = parser.parseIbCsv(csvFile(csv))

        val aaplTrades = trades["AAPL"]!!
        assertEquals("BUY", aaplTrades[0].buySell)
        assertEquals("SELL", aaplTrades[1].buySell)
    }

    @Test
    fun `parseIbCsv - multiple symbols are each grouped correctly`() {
        val csv = """
            ClientAccountID,Symbol,AssetClass,TradeDate,Quantity,TradePrice,CostBasis,IBCommission,FifoPnlRealized,Buy/Sell
            U123,AAPL,STK,20240101,10,150.00,-1500.00,-1.00,0.00,BUY
            U123,AAPL,STK,20240201,10,160.00,1600.00,-1.00,100.00,SELL
            U123,GOOG,STK,20240101,2,2800.00,-5600.00,-1.00,0.00,BUY
            U123,GOOG,STK,20240301,2,3000.00,6000.00,-1.00,400.00,SELL
        """.trimIndent()

        val (trades, _) = parser.parseIbCsv(csvFile(csv))

        assertEquals(2, trades.size)
        assertEquals(2, trades["AAPL"]!!.size)
        assertEquals(2, trades["GOOG"]!!.size)
    }

    @Test
    fun `parseIbCsv - quantity is stored as absolute value`() {
        val csv = """
            ClientAccountID,Symbol,AssetClass,TradeDate,Quantity,TradePrice,CostBasis,IBCommission,FifoPnlRealized,Buy/Sell
            U123,AAPL,STK,20240101,-10,150.00,-1500.00,-1.00,0.00,SELL
        """.trimIndent()

        val (trades, _) = parser.parseIbCsv(csvFile(csv))

        assertEquals(10, trades["AAPL"]!!.first().quantity)
    }

    @Test
    fun `parseIbCsv - handles quoted CSV fields correctly`() {
        val csv = """
            ClientAccountID,Symbol,AssetClass,TradeDate,Quantity,TradePrice,CostBasis,IBCommission,FifoPnlRealized,Buy/Sell
            U123,"AAPL",STK,20240101,10,"150.00","-1500.00","-1.00","0.00",BUY
            U123,"AAPL",STK,20240201,10,"160.00","1600.00","-1.00","100.00",SELL
        """.trimIndent()

        val (trades, _) = parser.parseIbCsv(csvFile(csv))

        assertTrue(trades.containsKey("AAPL"))
    }

    @Test
    fun `parseIbCsv - skips blank lines without error`() {
        val csv = """
            ClientAccountID,Symbol,AssetClass,TradeDate,Quantity,TradePrice,CostBasis,IBCommission,FifoPnlRealized,Buy/Sell
            
            U123,AAPL,STK,20240101,10,150.00,-1500.00,-1.00,0.00,BUY
            U123,AAPL,STK,20240201,10,160.00,1600.00,-1.00,100.00,SELL
        """.trimIndent()

        assertDoesNotThrow { parser.parseIbCsv(csvFile(csv)) }
    }

    @Test
    fun `parseIbCsv - malformed rows are silently skipped`() {
        val csv = """
            ClientAccountID,Symbol,AssetClass,TradeDate,Quantity,TradePrice,CostBasis,IBCommission,FifoPnlRealized,Buy/Sell
            U123,AAPL,STK,BADDATE,10,150.00,-1500.00,-1.00,0.00,BUY
            U123,AAPL,STK,20240201,10,160.00,1600.00,-1.00,100.00,SELL
        """.trimIndent()

        // malformed BUY is skipped; only the SELL remains → not a closed trade (no BUY counterpart)
        assertDoesNotThrow { parser.parseIbCsv(csvFile(csv)) }
    }

    // ─── CSV – Dividends ───────────────────────────────────────────────────────

    @Test
    fun `parseIbCsv - parses dividend records correctly`() {
        val csv = """
            ClientAccountID,Symbol,Date/Time,Amount,Type,Description
            U123,AAPL,20240115,0.92,Dividends,AAPL (US0378331005) Cash Dividend
        """.trimIndent()

        val (_, dividends) = parser.parseIbCsv(csvFile(csv))

        assertEquals(1, dividends.size)
        assertEquals("AAPL", dividends[0].symbol)
        assertEquals("0.92".toBigDecimal(), dividends[0].amount)
    }

    @Test
    fun `parseIbCsv - ignores non-dividend cash transaction types`() {
        val csv = """
            ClientAccountID,Symbol,Date/Time,Amount,Type,Description
            U123,AAPL,20240115,5.00,WithholdingTax,AAPL Tax
        """.trimIndent()

        val (_, dividends) = parser.parseIbCsv(csvFile(csv))

        assertTrue(dividends.isEmpty())
    }

    // ─── XML – Trades ──────────────────────────────────────────────────────────

    @Test
    fun `parseIbXml - returns closed stock trades grouped by symbol`() {
        val xml = """
            <?xml version="1.0"?>
            <FlexQueryResponse>
              <Trade symbol="AAPL" assetCategory="STK" tradeDate="20240101" quantity="10"
                     tradePrice="150.00" cost="-1500.00" ibCommission="-1.00"
                     fifoPnlRealized="0.00" buySell="BUY"/>
              <Trade symbol="AAPL" assetCategory="STK" tradeDate="20240201" quantity="-10"
                     tradePrice="160.00" cost="1600.00" ibCommission="-1.00"
                     fifoPnlRealized="100.00" buySell="SELL"/>
            </FlexQueryResponse>
        """.trimIndent()

        val (trades, _) = parser.parseIbXml(xmlFile(xml))

        assertEquals(1, trades.size)
        assertTrue(trades.containsKey("AAPL"))
    }

    @Test
    fun `parseIbXml - BUY-only symbol excluded from closed trades`() {
        val xml = """
            <?xml version="1.0"?>
            <FlexQueryResponse>
              <Trade symbol="MSFT" assetCategory="STK" tradeDate="20240101" quantity="5"
                     tradePrice="300.00" cost="-1500.00" ibCommission="-1.00"
                     fifoPnlRealized="0.00" buySell="BUY"/>
            </FlexQueryResponse>
        """.trimIndent()

        val (trades, _) = parser.parseIbXml(xmlFile(xml))

        assertTrue(trades.isEmpty())
    }

    @Test
    fun `parseIbXml - non-STK asset categories are ignored`() {
        val xml = """
            <?xml version="1.0"?>
            <FlexQueryResponse>
              <Trade symbol="SPY" assetCategory="OPT" tradeDate="20240101" quantity="1"
                     tradePrice="5.00" cost="-500.00" ibCommission="-0.50"
                     fifoPnlRealized="0.00" buySell="SELL"/>
            </FlexQueryResponse>
        """.trimIndent()

        val (trades, _) = parser.parseIbXml(xmlFile(xml))

        assertTrue(trades.isEmpty())
    }

    @Test
    fun `parseIbXml - quantity is stored as absolute value`() {
        val xml = """
            <?xml version="1.0"?>
            <FlexQueryResponse>
              <Trade symbol="AAPL" assetCategory="STK" tradeDate="20240101" quantity="-10"
                     tradePrice="160.00" cost="1600.00" ibCommission="-1.00"
                     fifoPnlRealized="100.00" buySell="SELL"/>
            </FlexQueryResponse>
        """.trimIndent()

        val (trades, _) = parser.parseIbXml(xmlFile(xml))

        assertEquals(10, trades["AAPL"]!!.first().quantity)
    }

    @Test
    fun `parseIbXml - trades are sorted by date within each symbol group`() {
        val xml = """
            <?xml version="1.0"?>
            <FlexQueryResponse>
              <Trade symbol="AAPL" assetCategory="STK" tradeDate="20240301" quantity="-10"
                     tradePrice="170.00" cost="1700.00" ibCommission="-1.00"
                     fifoPnlRealized="200.00" buySell="SELL"/>
              <Trade symbol="AAPL" assetCategory="STK" tradeDate="20240101" quantity="10"
                     tradePrice="150.00" cost="-1500.00" ibCommission="-1.00"
                     fifoPnlRealized="0.00" buySell="BUY"/>
            </FlexQueryResponse>
        """.trimIndent()

        val (trades, _) = parser.parseIbXml(xmlFile(xml))

        val aaplTrades = trades["AAPL"]!!
        assertEquals("BUY", aaplTrades[0].buySell)
        assertEquals("SELL", aaplTrades[1].buySell)
    }

    @Test
    fun `parseIbXml - malformed trade nodes are silently skipped`() {
        val xml = """
            <?xml version="1.0"?>
            <FlexQueryResponse>
              <Trade symbol="AAPL" assetCategory="STK" tradeDate="BADDATE" quantity="10"
                     tradePrice="150.00" cost="-1500.00" ibCommission="-1.00"
                     fifoPnlRealized="0.00" buySell="BUY"/>
              <Trade symbol="AAPL" assetCategory="STK" tradeDate="20240201" quantity="-10"
                     tradePrice="160.00" cost="1600.00" ibCommission="-1.00"
                     fifoPnlRealized="100.00" buySell="SELL"/>
            </FlexQueryResponse>
        """.trimIndent()

        assertDoesNotThrow { parser.parseIbXml(xmlFile(xml)) }
    }

    // ─── XML – Dividends ───────────────────────────────────────────────────────

    @Test
    fun `parseIbXml - parses dividend CashTransaction records`() {
        val xml = """
            <?xml version="1.0"?>
            <FlexQueryResponse>
              <CashTransaction symbol="AAPL" dateTime="20240115;120000" amount="0.92"
                               type="Dividends" description="AAPL Cash Dividend"/>
            </FlexQueryResponse>
        """.trimIndent()

        val (_, dividends) = parser.parseIbXml(xmlFile(xml))

        assertEquals(1, dividends.size)
        assertEquals("AAPL", dividends[0].symbol)
        assertEquals("0.92".toBigDecimal(), dividends[0].amount)
    }

    @Test
    fun `parseIbXml - ignores non-dividend CashTransaction types`() {
        val xml = """
            <?xml version="1.0"?>
            <FlexQueryResponse>
              <CashTransaction symbol="AAPL" dateTime="20240115;120000" amount="5.00"
                               type="WithholdingTax" description="Tax"/>
            </FlexQueryResponse>
        """.trimIndent()

        val (_, dividends) = parser.parseIbXml(xmlFile(xml))

        assertTrue(dividends.isEmpty())
    }

    @Test
    fun `parseIbXml - empty XML returns empty results`() {
        val xml = """
            <?xml version="1.0"?>
            <FlexQueryResponse/>
        """.trimIndent()

        val (trades, dividends) = parser.parseIbXml(xmlFile(xml))

        assertTrue(trades.isEmpty())
        assertTrue(dividends.isEmpty())
    }
}