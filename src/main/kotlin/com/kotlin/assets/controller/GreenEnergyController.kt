package com.kotlin.assets.controller

import com.kotlin.assets.service.GreenService
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@Controller
class GreenEnergyController(val greenService: GreenService) {

    @GetMapping("/")
    fun listFiles(model: Model): String {
//        model.addAttribute("files", uploadedFiles)
        return "solar"
    }

    @PostMapping(
        value = ["/green"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun calculateGreenReturn(
        @RequestPart("file") file: MultipartFile, model: Model
    ): String {
        if (!file.isEmpty) {
            try {
                greenService.calculateGreenReturn(file = file, model = model)
                model.addAttribute("message", "File uploaded successfully: ${file.originalFilename}")

            } catch (e: Exception) {
                model.addAttribute("message", "File upload failed: ${e.message}")
            }
        } else {
            model.addAttribute("message", "Cannot upload an empty file.")
        }
        return "solar"
    }
}