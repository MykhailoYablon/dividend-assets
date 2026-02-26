package com.kotlin.assets.controller

import com.kotlin.assets.dto.enums.FileType
import com.kotlin.assets.dto.tax.TotalTaxReportDto
import com.kotlin.assets.parser.IBFilesParser
import com.kotlin.assets.service.FileValidator
import com.kotlin.assets.service.TaxService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/taxes")
class TaxController(
    val taxService: TaxService,
    val fileParser: IBFilesParser,
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

    @GetMapping("/reports")
    fun generateXmlReports(@RequestParam year: Short) {
        taxService.generateXmlTaxReport(year)
    }
}