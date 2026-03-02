package com.kotlin.assets.service.impl

import com.kotlin.assets.dto.tax.xml.Declar
import com.kotlin.assets.dto.tax.xml.DeclarBody
import com.kotlin.assets.dto.tax.xml.DeclarBodyF1
import com.kotlin.assets.dto.tax.xml.DeclarHead
import com.kotlin.assets.dto.tax.xml.Doc
import com.kotlin.assets.dto.tax.xml.LinkedDocs
import com.kotlin.assets.entity.tax.TotalDividendReport
import com.kotlin.assets.entity.tax.TotalStockReport
import com.kotlin.assets.repository.ExchangeRateRepository
import com.kotlin.assets.repository.TotalDividendReportRepository
import com.kotlin.assets.repository.TotalStockReportRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class DeclarationGenerationService(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val xmlGeneratorService: XmlGeneratorService,
    private val totalStockReportRepository: TotalStockReportRepository,
    private val totalDividendReportRepository: TotalDividendReportRepository
) {

    private val exportDir = "exports"

    @Transactional
    fun generateXmlTaxReport(year: Short) {
        val totalStockReport = totalStockReportRepository.findByYear(year)
            .orElseThrow { throw ResponseStatusException(HttpStatus.NOT_FOUND) }

        val totalDividendReport = totalDividendReportRepository.findByYear(year)
            .orElseThrow { throw ResponseStatusException(HttpStatus.NOT_FOUND) }

        val uuid = UUID.randomUUID()
        val mainFilename = String.format("F0100214_Zvit_%s.xml", uuid)
        val f1Filename = String.format("F0121214_DodatokF1_%s.xml", uuid)

        val mainFilePath: String = exportDir + File.separator + mainFilename
        val f1FilePath: String = exportDir + File.separator + f1Filename

        val fillDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))

        val mainDeclar = createDeclar(uuid, totalStockReport, totalDividendReport, fillDate)
        val f1Declar = createDeclarF1(uuid, totalStockReport, fillDate)

        xmlGeneratorService.saveXmlToFile(mainDeclar, mainFilePath)
        xmlGeneratorService.saveXmlToFile(f1Declar, f1FilePath)

    }

    private fun createDeclar(
        uuid: UUID,
        totalStockReport: TotalStockReport,
        totalDividendReport: TotalDividendReport,
        fillDate: String
    ): Declar {
        val declar = Declar()

        val head = DeclarHead(
            tin = "", // IPN will add later
            cDoc = "F01",
            cDocSub = "002",
            cDocVer = "11",
            cDocType = "0",
            cDocCnt = "1",
            cReg = "0",
            cRaj = "15",
            periodMonth = "12",
            periodType = "5",
            periodYear = "2025",
            cStiOrig = "915",
            cDocStan = "1",
            dFill = fillDate // Дата заповнення
        )

        // Create linked docs
        val doc = Doc(
            num = "1",
            type = "1",
            cDoc = "F01",
            cDocSub = "212",
            cDocVer = "11",
            cDocType = "1",
            cDocCnt = "1",
            cDocStan = "1",
            filename = "F0121214_DodatokF1_$uuid.xml"
        )

        head.linkedDocs = LinkedDocs(docs = mutableListOf(doc))
        declar.declarHead = head

        // DECLARBODY
        val body = DeclarBody(
            h01 = "1",
            h03 = "1",
            h05 = "1",
            hbos = "Test", // Name
            hcity = "Test", // City
            hd1 = "1",
            hfill = fillDate, // Date of fill
            hname = "Test Test", // User name
            hsti = "ДПС", // ГОЛОВНЕ УПРАВЛІННЯ ДПС
            hstreet = "Stree",
            htin = "", // IPN
            hz = "1",
            hzy = "2025", //YEAR
            r0104g3 = totalDividendReport.totalUaBrutto, // Dividends totalUaBrutto
            r0104g6 = totalDividendReport.totalTax9, // Tax Dividends
            r0104g7 = totalDividendReport.totalMilitaryTax5,
            r0108g3 = totalStockReport.totalUaBrutto, // Інвест прибуток від акцій до податків
            r0108g6 = totalStockReport.totalTax18, // податок від акцій 18%
            r0108g7 = totalStockReport.totalMilitaryTax5, // військовий збір з акцій
            r01010g2s = "Інші проценти",
            r010g3 = totalDividendReport.totalUaBrutto + totalStockReport.totalUaBrutto, // Всього прибуток
            r010g6 = totalStockReport.totalTax18 + totalDividendReport.totalTax9, //всього податок акції + дивіденди
            r010g7 = totalDividendReport.totalMilitaryTax5 + totalStockReport.totalMilitaryTax5, // всього військовий збір
            r012g3 = totalDividendReport.totalUaBrutto + totalStockReport.totalUaBrutto, // Всього прибуток
            r013g3 = totalStockReport.totalTax18 + totalDividendReport.totalTax9, //всього податок акції + дивіденди
            r018g3 = BigDecimal.ZERO, //Сплачений податок у джерела 347.67 ??
            r0201g3 = BigDecimal.ZERO, // ?
            r0211g3 = totalDividendReport.totalMilitaryTax5 + totalStockReport.totalMilitaryTax5 // всього військовий збір
        )

        declar.setDeclarBody(body)

        return declar
    }

    private fun createDeclarF1(
        uuid: UUID,
        totalStockReport: TotalStockReport,
        fillDate: String
    ): Declar {

        val declar = Declar()

        // DECLARHEAD
        val head = DeclarHead(
            tin = "", // IPN
            cDoc = "F01",
            cDocSub = "212",
            cDocVer = "11",
            cDocType = "1",
            cDocCnt = "1",
            cReg = "0",
            cRaj = "15",
            periodMonth = "12",
            periodType = "5",
            periodYear = "2025",
            cStiOrig = "915",
            cDocStan = "1",
            dFill = fillDate // Date of fill
        )

        // Create linked docs
        val doc = Doc(
            num = "1",
            type = "2",
            cDoc = "F01",
            cDocSub = "002",
            cDocVer = "11",
            cDocType = "0",
            cDocCnt = "1",
            cDocStan = "1",
            filename = "F0100214_Zvit_$uuid.xml"
        )

        head.linkedDocs = LinkedDocs(docs = mutableListOf(doc))
        declar.declarHead = head

        // DECLARBODY F1
        val body = DeclarBodyF1(
            hbos = "Test Test",
            htin = "", // IPN
            hz = "1",
            hzy = "2025", // year
            r001g4 = totalStockReport.totalSell, // Продаж
            r001g5 = totalStockReport.totalBuy, // Купівля
            r001g6 = totalStockReport.totalUaBrutto, // Інвест прибуток 933.81
            r003g6 = totalStockReport.totalUaBrutto, // Інвест прибуток 933.81
            r004g6 = totalStockReport.totalTax18, // Усього до сплати податку 168.09
            r005g6 = totalStockReport.totalMilitaryTax5, // до сплати військового збору
            r031g6 = totalStockReport.totalUaBrutto,
            r042g6 = totalStockReport.totalTax18,
            r052g6 = totalStockReport.totalMilitaryTax5
        )

        // Add table rows
        totalStockReport.records.forEachIndexed { index, record ->
            body.addTableRow((index + 1).toString(),
                "4", // ??
                record.symbol + " " + record.sellQuantity +"шт.",
                record.sellPriceUah.toString(),
                record.buyPriceUah.toString(),
                record.netProfitUah.toString())
        }
        declar.setDeclarBody(body)
        return declar
    }
}