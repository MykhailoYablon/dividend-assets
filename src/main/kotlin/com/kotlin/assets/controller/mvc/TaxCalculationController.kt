package com.kotlin.assets.controller.mvc

import com.kotlin.assets.dto.MyUserDetails
import com.kotlin.assets.service.utils.FileValidator
import com.kotlin.assets.service.impl.DeclarationGenerationService
import com.kotlin.assets.service.impl.TaxService
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import java.util.Locale

@Controller
class TaxCalculationController(
    val taxService: TaxService,
    val fileValidator: FileValidator,
    val declarationGenerationService: DeclarationGenerationService
) {
    @GetMapping("/")
    fun getAllReports(
        model: Model,
        @RequestParam(required = false, defaultValue = "2025") year: Short,
        @AuthenticationPrincipal user: MyUserDetails,
        locale: Locale
    ): String {
        val taxReports = taxService.getTaxReports(year)
        model.addAttribute("taxReports", taxReports)
        model.addAttribute("year", year)

        System.out.println("Current Locale: " + locale.getLanguage());
        return "main"
    }

    @PostMapping(
        value = ["/calculate"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun calculateDividendTax(
        @RequestParam(required = true) year: Short,
        @RequestPart("file", required = true) file: MultipartFile,
        @RequestParam(required = false, defaultValue = "false") isMilitary: Boolean
    ): String {
        val fileType = fileValidator.validate(file)
        taxService.calculateTax(year, file, fileType, isMilitary)
        return "redirect:/?year=$year"
    }

    @GetMapping("/stocks")
    fun stocksPage(
        model: Model,
        @RequestParam(required = false, defaultValue = "2025") year: Short
    ): String {
        model.addAttribute("report", taxService.getTaxReports(year)?.totalStockReport)
        model.addAttribute("year", year)
        return "stocks"
    }

    @GetMapping("/dividends")
    fun dividendsPage(
        model: Model,
        @RequestParam(required = false, defaultValue = "2025") year: Short
    ): String {
        model.addAttribute("report", taxService.getTaxReports(year)?.totalDividendReport)
        model.addAttribute("year", year)
        return "dividends"
    }

    @PostMapping("/stocks/delete")
    fun deleteStockReport(@RequestParam year: Short): String {
        taxService.deleteStockReport(year)
        return "redirect:/?year=$year"
    }

    @PostMapping("/dividends/delete")
    fun deleteDividendReport(@RequestParam year: Short): String {
        taxService.deleteDividendReport(year)
        return "redirect:/?year=$year"
    }

    @GetMapping("/declaration")
    fun declarationForm(
        model: Model,
        @RequestParam(required = false, defaultValue = "2025") year: Short
    ): String {

        val dpis = declarationGenerationService.getAllDpis()

        model.addAttribute("year", year)

        model.addAttribute("dpiList", dpis)

        return "declaration"
    }

    @PostMapping("/declaration", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun generateDeclaration(
        @RequestParam year: Short,
        @RequestParam fullName: String,
        @RequestParam ipn: String,
        @RequestParam taxNameId: Long,
        @RequestParam city: String,
        @RequestParam street: String
    ): ResponseEntity<ByteArray> {
        val zipBytes = declarationGenerationService.generateXmlZip(year, fullName, ipn, taxNameId, city, street)
        val headers = HttpHeaders().apply {
            contentDisposition = ContentDisposition.attachment()
                .filename("declaration_$year.zip")
                .build()
            contentType = MediaType.parseMediaType("application/zip")
        }
        return ResponseEntity(zipBytes, headers, HttpStatus.OK)
    }
}