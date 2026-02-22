package com.kotlin.assets.controller

import com.kotlin.assets.dto.MyUserDetails
import com.kotlin.assets.service.FileValidator
import com.kotlin.assets.service.SolarService
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@Controller
class SolarEnergyController(
    val solarService: SolarService,
    val fileValidator: FileValidator
) {

    @GetMapping("/")
    fun getAllReports(model: Model): String {
        solarService.getAllReports(model)
        return "solar"
    }

    @PostMapping(
        value = ["/green"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun calculateGreenReturn(
        @RequestPart("file") file: MultipartFile, model: Model,
        @AuthenticationPrincipal user: MyUserDetails
    ): String {
        if (!file.isEmpty) {
            if (file.size > 10 * 1024 * 1024) {
                throw IllegalArgumentException("File too large")
            }
            fileValidator.validate(file)
            val safeFilename = StringUtils.cleanPath(file.originalFilename ?: "unknown")
                .replace("..", "") // prevent path traversal

            solarService.calculateGreenReturn(file = file, model = model, fileName = safeFilename, userId = user.getId())

            model.addAttribute("message", "File uploaded successfully: $safeFilename")
        } else {
            model.addAttribute("message", "Cannot upload an empty file.")
        }
        return "solar"
    }
}