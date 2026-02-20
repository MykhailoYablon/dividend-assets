package com.kotlin.assets.controller

import com.kotlin.assets.dto.TotalTaxReportDto
import com.kotlin.assets.service.TaxService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/taxes")
class TaxController(val taxService: TaxService) {

    @PostMapping(
        value = ["/dividends"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun calculateDividendTax(
        @RequestParam year: Short,
        @RequestPart("file") file: MultipartFile,
        @RequestParam isMilitary: Boolean
    ): TotalTaxReportDto {
        return taxService.calculateDividendTax(year, file, isMilitary)
    }

    @GetMapping("/reports")
    fun generateXmlReports(@RequestParam year: Short) {
        taxService.generateXmlTaxReport(year)
    }
}