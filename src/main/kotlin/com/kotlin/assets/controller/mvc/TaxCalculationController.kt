package com.kotlin.assets.controller.mvc

import com.kotlin.assets.dto.MyUserDetails
import com.kotlin.assets.dto.tax.TotalTaxReportDto
import com.kotlin.assets.service.utils.FileValidator
import com.kotlin.assets.service.impl.TaxService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.multipart.MultipartFile

@Controller
class TaxCalculationController(
    val taxService: TaxService,
    val fileValidator: FileValidator
) {
    @GetMapping("/")
    fun getAllReports(
        model: Model,
        @RequestParam(required = false, defaultValue = "2025") year: Short,
        @AuthenticationPrincipal user: MyUserDetails
    ): String {
        taxService.getTaxReports(model, year, user.getId())
        return "main"
    }

    @PostMapping(
        value = ["/tax"],
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
}