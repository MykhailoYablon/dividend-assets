package com.kotlin.assets.service

import org.apache.tika.Tika
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class FileValidator {

    private val tika = Tika()

    private val allowedTypes = setOf(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/x-tika-ooxml"
    )

    fun validate(file: MultipartFile) {
        val detectedType = tika.detect(file.inputStream)
        if (detectedType !in allowedTypes) {
            throw IllegalArgumentException("Only xlsx files are allowed")
        }
    }
}