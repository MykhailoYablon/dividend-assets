package com.kotlin.assets.controller

import com.kotlin.assets.dto.TotalTaxReportDto
import com.kotlin.assets.service.TaxService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/taxes")
class TaxController(val taxService: TaxService) {

    @PostMapping("/dividends")
    fun calculateDividendTax(year: Short, file: MultipartFile, isMilitary: Boolean): TotalTaxReportDto {
        return taxService.calculateDividendTax(year, file, isMilitary)
    }
}