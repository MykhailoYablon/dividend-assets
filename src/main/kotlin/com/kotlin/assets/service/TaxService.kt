package com.kotlin.assets.service

import com.kotlin.assets.dto.ib.DividendRecord
import com.kotlin.assets.dto.enums.FileType
import com.kotlin.assets.dto.enums.ReportStatus
import com.kotlin.assets.dto.ib.TradeRecord
import com.kotlin.assets.dto.tax.TotalTaxReportDto
import com.kotlin.assets.dto.tax.xml.*
import com.kotlin.assets.entity.DividendTaxReport
import com.kotlin.assets.entity.TotalTaxReport
import com.kotlin.assets.mapper.TaxReportMapper
import com.kotlin.assets.parser.IBFilesParser
import com.kotlin.assets.repository.TotalTaxReportRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@Service
class TaxService(
    private val parser: IBFilesParser,
    private val exchangeRateService: ExchangeRateService,
    private val totalTaxReportRepository: TotalTaxReportRepository,
    private val taxReportMapper: TaxReportMapper,
    private val xmlGeneratorService: XmlGeneratorService
) {

    val scale = 2
    val roundingMode: RoundingMode = RoundingMode.HALF_DOWN
    val exportDir = "exports"

    @Transactional
    fun calculateTax(
        year: Short,
        file: MultipartFile,
        fileType: FileType,
        isMilitary: Boolean
    ): TotalTaxReportDto {
        val ibReport = when (fileType) {
            FileType.CSV  -> parser.parseIbCsv(file)
            FileType.XML  -> parser.parseIbXml(file)
            FileType.XLSX -> throw IllegalArgumentException("XLSX not supported yet")
        }

        val dividends = ibReport.second

        val rateCache = dividends.map { it.date }
            .toSet()
            .associateWith { date -> exchangeRateService.getRateForDate(date) }

        var totalAmount = BigDecimal.ZERO
        var totalUaBrutto = BigDecimal.ZERO
        val reports: MutableList<DividendTaxReport> = mutableListOf()

        dividends.forEach { dividend ->
            val dividendAmount = dividend.amount
            val dividendDate = dividend.date
            val exchangeRate = rateCache[dividendDate] ?: BigDecimal.ZERO
            val uaBrutto = exchangeRate.multiply(dividendAmount)
                .setScale(scale, roundingMode)

            totalAmount += dividendAmount
            totalUaBrutto += uaBrutto

            val tax9: BigDecimal = uaBrutto.multiply(BigDecimal.valueOf(0.09))
                .setScale(scale, roundingMode)
            val militaryTax5 = uaBrutto.multiply(BigDecimal.valueOf(0.05))
                .setScale(scale, roundingMode)
            //if you are in military then don't
            val taxSum: BigDecimal = if (isMilitary) tax9 else tax9.add(militaryTax5)

            reports.add(
                DividendTaxReport(
                    symbol = dividend.symbol,
                    date = dividendDate,
                    amount = dividendAmount,
                    nbuRate = exchangeRate,
                    uaBrutto = uaBrutto,
                    tax9 = tax9,
                    militaryTax5 = militaryTax5,
                    taxSum = taxSum
                )
            )
        }

        // Calculate tax on taxReport (not sum of individual taxes)
        val totalTax9 = round(totalUaBrutto.multiply(BigDecimal("0.09")))
        val totalMilitaryTax5 = round(totalUaBrutto.multiply(BigDecimal("0.05")))
        val totalTaxSum = round(totalTax9.add(totalMilitaryTax5))

        val totalTaxReport =
            totalTaxReportRepository.findByYear(year).orElseGet {
                TotalTaxReport(
                    year = year, status = ReportStatus.CALCULATED
                )
            }

        totalTaxReport.apply {
            this.totalAmount = totalAmount
            this.totalUaBrutto = totalUaBrutto
            this.totalTax9 = totalTax9
            this.totalMilitaryTax5 = totalMilitaryTax5
            this.totalTaxSum = totalTaxSum
            this.setTaxReports(reports)
        }

        val taxReport = totalTaxReportRepository.save(totalTaxReport)

        return taxReportMapper.toDto(taxReport)
    }

    private fun round(value: BigDecimal): BigDecimal {
        return value.setScale(2, RoundingMode.HALF_UP)
    }

    fun generateXmlTaxReport(year: Short) {
        val totalTaxReport = totalTaxReportRepository.findByYear(year)
            .orElseThrow { throw ResponseStatusException(HttpStatus.NOT_FOUND) }

        val uuid = UUID.randomUUID()
        val mainFilename = String.format("F0100214_Zvit_%s.xml", uuid)
        val f1Filename = String.format("F0121214_DodatokF1_%s.xml", uuid)

        val mainFilePath: String = exportDir + File.separator + mainFilename
        val f1FilePath: String = exportDir + File.separator + f1Filename

        val mainDeclar = createDeclar(uuid, totalTaxReport)
        val f1Declar = createDeclarF1(uuid, totalTaxReport)

        xmlGeneratorService.saveXmlToFile(mainDeclar, mainFilePath)
        xmlGeneratorService.saveXmlToFile(f1Declar, f1FilePath)

    }

    private fun createDeclar(uuid: UUID, totalTaxReport: TotalTaxReport): Declar {
        val declar = Declar()

        val totalMilitaryTax5 = totalTaxReport.totalMilitaryTax5


        val head = DeclarHead(
            tin = "", // IPN
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
            dFill = "16012026" // Дата заповнення
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
            hfill = "16012026", // Date of fill
            hname = "Test Test", // User name
            hsti = "ДПС", // ГОЛОВНЕ УПРАВЛІННЯ ДПС
            hstreet = "Stree",
            htin = "", // IPN
            hz = "1",
            hzy = "2025",
            r0104g3 = totalTaxReport.totalUaBrutto, // Dividends totalUaBrutto
            r0104g6 = totalTaxReport.totalTax9, // Tax Dividends
            r0104g7 = totalTaxReport.totalMilitaryTax5,
            r0108g3 = BigDecimal.ZERO, // Інвест прибуток від акцій до податків
            r0108g6 = BigDecimal.ZERO, // до сплати податку від акцій
            r0108g7 = BigDecimal.ZERO, // військовий збір з акцій
            r01010g2s = "Інші проценти",
            r010g3 = BigDecimal.ZERO, // Всього прибуток
            r010g6 = BigDecimal.ZERO, //всього податок акції + дивіденди
            r010g7 = BigDecimal.ZERO, // всього військовий збір
            r012g3 = BigDecimal.ZERO, // Всього прибуток
            r013g3 = BigDecimal.ZERO, //всього податок акції + дивіденди
            r018g3 = BigDecimal.ZERO, //Сплачений податок у джерела 347.67
            r0201g3 = BigDecimal.ZERO, // ?
            r0211g3 = BigDecimal.ZERO // всього військовий збір
        )

        declar.setDeclarBody(body)

        return declar
    }

    private fun createDeclarF1(uuid: UUID, totalTaxReport: TotalTaxReport): Declar {

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
            dFill = "16012026" // Date of fill
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
            r001g4 = "32130.56", // Продаж ?
            r001g5 = "31196.75", // Купівля ?
            r001g6 = "933.81", // Інвест прибуток 933.81
            r003g6 = "933.81", // Інвест прибуток 933.81
            r004g6 = "168.09", // Усього до сплати податку 168.09
            r005g6 = "46.69", // до сплати військового збору
            r031g6 = "933.81",
            r042g6 = "168.09",
            r052g6 = "46.69"
        )

        // Add table rows
        body.addTableRow("1", "4", "GOOGL 1шт.", "6903.66", "6259.61", "644.05")
        body.addTableRow("2", "4", "CRM 1шт.", "11047.23", "11016.3", "30.93")
        body.addTableRow("3", "4", "VTI 1шт.", "14179.67", "13920.84", "258.83")

        declar.setDeclarBody(body)

        return declar
    }
}