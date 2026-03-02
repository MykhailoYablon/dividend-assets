package com.kotlin.assets.controller

import com.kotlin.assets.dto.tax.TotalTaxReportDto
import com.kotlin.assets.service.impl.DeclarationGenerationService
import com.kotlin.assets.service.utils.FileValidator
import com.kotlin.assets.service.impl.TaxService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/taxes")
class TaxController(
    val taxService: TaxService,
    val declarationGenerationService: DeclarationGenerationService,
    val fileValidator: FileValidator
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

    @GetMapping("/declaration")
    fun generateXmlReports(@RequestParam year: Short) {
        declarationGenerationService.generateXmlTaxReport(year)
    }
}