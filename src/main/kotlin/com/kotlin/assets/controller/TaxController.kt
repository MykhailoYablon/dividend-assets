package com.kotlin.assets.controller

import com.kotlin.assets.dto.tax.TotalTaxReportDto
import com.kotlin.assets.parser.DpiExcelParserService
import com.kotlin.assets.service.impl.DeclarationGenerationService
import com.kotlin.assets.service.impl.TaxService
import com.kotlin.assets.service.utils.FileValidator
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/taxes")
class TaxController(
    private val taxService: TaxService,
    private val declarationGenerationService: DeclarationGenerationService,
    private val fileValidator: FileValidator,
    private val parserService: DpiExcelParserService
) {

    @PostMapping(
        value = ["/dividends"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun calculateDividendTax(
        @RequestParam(required = true) year: Short,
        @RequestPart("file", required = true) file: MultipartFile,
        @RequestParam(required = false) isMilitary: Boolean
    ): TotalTaxReportDto {
        val fileType = fileValidator.validate(file)
        return taxService.calculateTax(year, file, fileType, isMilitary)
    }

    @PostMapping("/declaration")
    fun generateXmlReports(
        @RequestParam year: Short,
        @RequestParam fullName: String,
        @RequestParam ipn: String,
        @RequestParam taxName: String,
        @RequestParam city: String,
        @RequestParam street    : String
    ) {
        declarationGenerationService.generateXmlTaxReport(year)
    }

    @PostMapping("/api/dpi/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        parserService.parseAndSave(file.inputStream)
        return ResponseEntity.ok("Imported successfully")
    }
}